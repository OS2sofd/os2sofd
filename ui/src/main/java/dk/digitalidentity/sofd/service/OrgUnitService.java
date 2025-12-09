package dk.digitalidentity.sofd.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import dk.digitalidentity.sofd.config.RoleConstants;
import dk.digitalidentity.sofd.config.SessionConstants;
import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.dto.PUnitDTO;
import dk.digitalidentity.sofd.controller.rest.model.OrgUnitCoreInfo;
import dk.digitalidentity.sofd.dao.OrgUnitDao;
import dk.digitalidentity.sofd.dao.OrgUnitTypeDao;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.Ean;
import dk.digitalidentity.sofd.dao.model.ModificationHistory;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.OrgUnitType;
import dk.digitalidentity.sofd.dao.model.Organisation;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.Phone;
import dk.digitalidentity.sofd.dao.model.Post;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.mapping.OrgUnitPostMapping;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.model.ChangeType;
import dk.digitalidentity.sofd.service.model.KleAssignmentDto;
import dk.digitalidentity.sofd.service.model.KleAssignmentType;
import dk.digitalidentity.sofd.service.model.OUAddressRow;
import dk.digitalidentity.sofd.service.model.OUAddressTreeNode;
import dk.digitalidentity.sofd.service.model.OUTagRow;
import dk.digitalidentity.sofd.service.model.OUTreeForm;
import dk.digitalidentity.sofd.service.model.OUTreeFormWithTags;
import dk.digitalidentity.sofd.service.model.SubstituteOrgUnitAssignmentDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class OrgUnitService {
	private static OrgUnitService instance;

	/*
	 * SELECT o.uuid, o.name, o.parent_uuid
	 *   FROM orgunits o
	 *   WHERE o.deleted = 0
	 *   AND o.belongs_to = ?;
	 */
	private static final String SELECT_THIN_ORGUNITS_SQL = "SELECT o.uuid, o.name AS name, o.parent_uuid FROM orgunits o WHERE o.deleted = 0 AND o.belongs_to = ?;";

	/*
		SELECT o.uuid, o.name AS name, o.parent_uuid, t.id AS tag_id, ifnull(mp.chosen_name,concat(mp.firstname ,'',mp.surname)) AS manager
		FROM orgunits o
		LEFT JOIN orgunits_tags ot ON ot.orgunit_uuid = o.uuid
		LEFT JOIN tags t ON ot.tag_id = t.id
		LEFT JOIN orgunits_manager m ON m.orgunit_uuid = o.uuid
		LEFT JOIN persons mp on mp.uuid = m.manager_uuid
		WHERE o.deleted = 0 AND o.belongs_to = ?;
	*/
	private static final String SELECT_THIN_ORGUNITS_SQL_WITH_TAGS_AND_MANAGER = "SELECT o.uuid, o.name AS name, o.parent_uuid, t.id AS tag_id, ifnull(mp.chosen_name,concat(mp.firstname ,' ',mp.surname)) AS manager, ot.custom_value AS tag_value FROM orgunits o LEFT JOIN orgunits_tags ot ON ot.orgunit_uuid = o.uuid LEFT JOIN tags t ON ot.tag_id = t.id LEFT JOIN orgunits_manager m ON m.orgunit_uuid = o.uuid LEFT JOIN persons mp on mp.uuid = m.manager_uuid WHERE o.deleted = 0 AND o.belongs_to = ?;";


	/*
	 * SELECT o.uuid, o.name, CONCAT(p.postal_code, ' ', p.city) AS city, CONCAT(p.street, ', ', p.postal_code, ' ', p.city) AS address
	 *   FROM orgunits o
     *   JOIN orgunits_posts op ON op.orgunit_uuid = o.uuid
     *   JOIN posts p ON op.post_id = p.id
     *   WHERE o.deleted = 0
     * 	 AND o.belongs_to = ?;
	 */
	private static final String SELECT_THIN_ORGUNITS_WITH_ADDRESS_SQL = "SELECT o.uuid, o.name, CONCAT(p.postal_code, ' ', p.city) AS city, CONCAT(p.street, ', ', p.postal_code, ' ', p.city) AS address FROM orgunits o JOIN orgunits_posts op ON op.orgunit_uuid = o.uuid JOIN posts p ON op.post_id = p.id WHERE o.deleted = 0 AND o.belongs_to = ?;";

	public static OrgUnitService getInstance() {
		return instance;
	}

	@PostConstruct
	public void init() {
		instance = this;
	}

	@Qualifier("defaultTemplate")
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private OrgUnitDao orgUnitDao;

	@Autowired
	private OrgUnitTypeDao orgUnitTypeDao;

	@Autowired
	private ModificationHistoryService modificationHistoryService;
	
	@Autowired
	private KleService kleService;

	@Autowired
	private OrgUnitService self;

	@Autowired
	private OrganisationService organisationService;

	@Autowired
	private OrgUnitFutureChangesService orgUnitFutureChangesService;

	@Autowired
	private CvrService cvrService;

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private ManagerService managerService;

	@Autowired
	AffiliationService affiliationService;

	@Autowired
	private EanService eanService;

	@Autowired
	private ObjectMapper mapper;

	public OrgUnit getByUuid(String uuid) {
		Date date = getFutureDateFromSession();
		if (date != null) {
			return orgUnitFutureChangesService.getFutureOrgUnit(uuid, date);
		}
		var orgUnit = orgUnitDao.findByUuid(uuid);

		if (orgUnit != null && orgUnit.getEanList().isEmpty()) {
			List<Long> ids = orgUnitDao.getInheritedEan(orgUnit.getUuid());
			if (!ids.isEmpty()) {
				ids.removeIf(Objects::isNull);
				orgUnit.setInheritedEan(true);
				orgUnit.setInheritedEanList(new ArrayList<>());
				
				for (long eanId : ids) {
					Ean sourceEan = eanService.findById(eanId);
					Ean ean = new Ean(0, sourceEan.getNumber(), "INHERITED", sourceEan.isPrime(), orgUnit);
					orgUnit.getInheritedEanList().add(ean);
				}
			}
		}

		return orgUnit;
	}

	public Set<String> getDoNotTransferToFKOrgUuids() {
		return orgUnitDao.getDoNotTransferToFKOrgUuids();
	}

	public Page<OrgUnit> getAll(Pageable pageable) {
		return orgUnitDao.findByBelongsTo(organisationService.getAdmOrg(), pageable);
	}

	public List<OrgUnit> getAllWithChildren(List<String> list) {
		List<OrgUnit> ous = orgUnitDao.findByUuidIn(list);
		Set<OrgUnit> result = new HashSet<>();
		for (OrgUnit ou : ous) {
			result.add(ou);
			for (OrgUnit child : ou.getChildren()) {
				getAllWithChildrenRecursive(result, child);
			}
		}

		return new ArrayList<>(result);
	}

	private void getAllWithChildrenRecursive(Set<OrgUnit> result, OrgUnit ou) {
		result.add(ou);
		for (OrgUnit child : ou.getChildren()) {
			getAllWithChildrenRecursive(result, child);
		}
	}

	public List<OrgUnit> getByUuid(List<String> uuids) {
		Date date = getFutureDateFromSession();
		if (date != null) {
			return orgUnitFutureChangesService.getFutureOrgUnits(uuids, date);
		}

		return orgUnitDao.findByUuidIn(uuids);
	}

	public List<OrgUnit> getBySourceName(String sourceName) {

		return orgUnitDao.findBySourceNameAndDeleted(sourceName,false);
	}

	public List<OrgUnit> getAll() {
		return getAll(organisationService.getAdmOrg());
	}

	@Transactional
	public List<OrgUnit> getAll(Consumer<OrgUnit> consumer) {
		List<OrgUnit> orgUnits = getAll(organisationService.getAdmOrg());
		
		orgUnits.forEach(consumer);
		
		return orgUnits;
	}

	public List<OrgUnit> getAll(Organisation organisation) {
		Date date = getFutureDateFromSession();
		if (date != null) {
			return orgUnitFutureChangesService.getAllFutureOrgUnits(organisation, date);
		}

		return orgUnitDao.findByBelongsTo(organisation);
	}


	public List<OrgUnit> getAllActiveCached() {
		return self.getAllActiveCached(organisationService.getAdmOrg());
	}

	@Cacheable(value = "activeOrgUnits")
	public List<OrgUnit> getAllActiveCached(Organisation organisation) {
		var result = getAllActive(organisation);
		// force load type
		result.forEach(o -> o.getType().getKey());
		return result;
	}

	@Scheduled(fixedRate = 60 * 60 * 1000)
	public void resetActiveOrgUnitCacheTask() {
		self.resetActiveOrgUnitCache();
	}

	@CacheEvict(value = "activeOrgUnits", allEntries = true)
	public void resetActiveOrgUnitCache() {
		; // clears cache every hour - we want to protect
		  // against force-refresh in the browser, as the lookup
		  // can be a bit intensive
	}

	public List<OrgUnit> getAllActive() {
		return getAllActive(organisationService.getAdmOrg());
	}

	public List<OrgUnit> getAllActive(Organisation organisation) {
		Date date = getFutureDateFromSession();
		if (date != null) {
			return orgUnitFutureChangesService.getAllActiveFutureOrgUnits(organisation, date);
		}

		return orgUnitDao.findByDeletedFalseAndBelongsTo(organisation);
	}

	public List<OUTreeForm> getAllTree() {
		return getAllTree(organisationService.getAdmOrg());
	}

	public List<OUTreeForm> getAllExceptAdmOrg() {
		List<OUTreeForm> result = new ArrayList<>();
		for (Organisation organisation : organisationService.getAllExceptAdmOrg()) {
			result.addAll(jdbcTemplate.query(SELECT_THIN_ORGUNITS_SQL, new Object[] { organisation.getId()},(RowMapper<OUTreeForm>) (rs, rownum) -> {
				OUTreeForm ou = new OUTreeForm();

				ou.setId(rs.getString("uuid"));
				ou.setText(rs.getString("name"));

				String parent_uuid = rs.getString("parent_uuid");
				if (parent_uuid != null && !parent_uuid.isEmpty()) {
					ou.setParent(parent_uuid);
				}
				else {
					ou.setParent("#");
				}

				return ou;
			}));
		}
		result.sort(Comparator.comparing(OUTreeForm::getText));
		return result;
	}

	public List<OUTreeFormWithTags> getAllTreeWithTags() {
		return getAllTreeWithTags(organisationService.getAdmOrg());
	}

	public List<OUTreeForm> getAllTree(Organisation organisation) {
		@SuppressWarnings("deprecation")
		List<OUTreeForm> result = jdbcTemplate.query(SELECT_THIN_ORGUNITS_SQL, new Object[] { organisation.getId()},(RowMapper<OUTreeForm>) (rs, rownum) -> {
			OUTreeForm ou = new OUTreeForm();

			ou.setId(rs.getString("uuid"));
			ou.setText(rs.getString("name"));

			String parent_uuid = rs.getString("parent_uuid");
			if (parent_uuid != null && !parent_uuid.isEmpty()) {
				ou.setParent(parent_uuid);
			}
			else {
				ou.setParent("#");
			}

			return ou;
		});

		Date date = getFutureDateFromSession();
		if (date != null) {
			result = orgUnitFutureChangesService.getAllTreeFutureOrgUnits(result, date);
		}

		result.sort(Comparator.comparing(OUTreeForm::getText));

		return result;
	}

	public List<OUTreeFormWithTags> getAllTreeWithTags(Organisation organisation) {
		@SuppressWarnings("deprecation")
		List<OUTagRow> result = jdbcTemplate.query(SELECT_THIN_ORGUNITS_SQL_WITH_TAGS_AND_MANAGER, new Object[] { organisation.getId()}, (RowMapper<OUTagRow>) (rs, rownum) -> {
			OUTagRow ou = new OUTagRow();

			ou.setId(rs.getString("uuid"));
			ou.setText(rs.getString("name"));
			ou.setManager(rs.getString("manager"));
			ou.setTagId(rs.getLong("tag_id"));
			ou.setTagValue(rs.getString("tag_value"));

			String parentUuid = rs.getString("parent_uuid");
			if (parentUuid != null && !parentUuid.isEmpty()) {
				ou.setParent(parentUuid);
			}
			else {
				ou.setParent("#");
			}

			return ou;
		});

		List<OUTreeFormWithTags> treeFormList = new ArrayList<>();

		for (OUTagRow ou : result) {
			OUTreeFormWithTags match = treeFormList.stream().filter(t -> t.getId().equals(ou.getId())).findAny().orElse(null);

			if (match == null) {
				OUTreeFormWithTags newForm = new OUTreeFormWithTags();
				newForm.setId(ou.getId());
				newForm.setParent(ou.getParent());
				newForm.setText(ou.getText());
				newForm.setManager(ou.getManager());
				newForm.setTagIds(new ArrayList<>());
				newForm.setTagValueMap(new HashMap<>());

				if (ou.getTagId() != null) {
					newForm.getTagIds().add(ou.getTagId());
					if (ou.getTagValue() != null) {
						newForm.getTagValueMap().put(ou.getTagId(), ou.getTagValue());
					}
				}

				treeFormList.add(newForm);
			} else {
				if (ou.getTagId() != null) {
					match.getTagIds().add(ou.getTagId());
					if (ou.getTagValue() != null) {
						match.getTagValueMap().put(ou.getTagId(), ou.getTagValue());
					}
				}
			}
		}

		Date date = getFutureDateFromSession();
		if (date != null) {
			treeFormList = orgUnitFutureChangesService.getAllTreeWithTagsFutureOrgUnits(treeFormList, date);
		}

		treeFormList.sort(Comparator.comparing(OUTreeFormWithTags::getText));

		return treeFormList;
	}

	public List<KleAssignmentDto> getKleAssignments(OrgUnit orgUnit, KleAssignmentType type) {
		List<KleAssignmentDto> assignments = new ArrayList<>();

		copyKleToAssignments(assignments, orgUnit, type, false);

		if (orgUnit.getParent() != null) {
			getKleAssignmentsInherited(assignments, orgUnit.getParent(), type);
		}

		return assignments;
	}

	private void getKleAssignmentsInherited(List<KleAssignmentDto> assignments, OrgUnit orgUnit, KleAssignmentType type) {
		if (orgUnit.isInheritKle()) {
			copyKleToAssignments(assignments, orgUnit, type, true);
		}

		if (orgUnit.getParent() != null) {
			getKleAssignmentsInherited(assignments, orgUnit.getParent(), type);
		}
	}

	private void copyKleToAssignments(List<KleAssignmentDto> assignments, OrgUnit orgUnit, KleAssignmentType type, boolean inherited) {
		List<String> codes = new ArrayList<>();
		switch (type) {
			case PRIMARY:
				if (orgUnit.getKlePrimary() != null) {
					codes = orgUnit.getKlePrimary().stream().map(k -> k.getKleValue()).collect(Collectors.toList());
				}
				break;
			case SECONDARY:
				if (orgUnit.getKleSecondary() != null) {
					codes = orgUnit.getKleSecondary().stream().map(k -> k.getKleValue()).collect(Collectors.toList());
				}
				break;
			case TERTIARY:
				if (orgUnit.getKleTertiary() != null) {
					codes = orgUnit.getKleTertiary().stream().map(k -> k.getKleValue()).collect(Collectors.toList());
				}
				break;
		}

		if (codes != null && codes.size() > 0) {
			for (String code : codes) {
				String title = kleService.getName(code);

				KleAssignmentDto assignment = new KleAssignmentDto();
				assignment.setCode(code);
				assignment.setTitle(title);

				if (inherited) {
					assignment.setThrough(orgUnit.getName());
				}

				assignments.add(assignment);
			}
		}
	}

	// used by OS2sync, and is hardcoded to the 3rd KLE markup
	public Set<String> getKleCodesIncludingInherited(OrgUnit orgUnit) {
		Set<String> codes = new HashSet<>();

		if (orgUnit.getKleTertiary() != null && orgUnit.getKleTertiary().size() > 0) {
			for (String code : orgUnit.getKleTertiary().stream().map(k -> k.getKleValue()).collect(Collectors.toList())) {
				codes.add(code);
			}
		}

		if (orgUnit.getParent() != null) {
			addInheritedKle(orgUnit.getParent(), codes);
		}

		return codes;
	}

	private void addInheritedKle(OrgUnit parent, Set<String> codes) {
		if (parent.getParent() != null) {
			addInheritedKle(parent.getParent(), codes);
		}

		if (parent.isInheritKle()) {
			if (parent.getKleTertiary() != null && parent.getKleTertiary().size() > 0) {
				for (String code : parent.getKleTertiary().stream().map(k -> k.getKleValue()).collect(Collectors.toList())) {
					codes.add(code);
				}
			}
		}
	}

	/**
	 * Never affected by future organisation changes, as it is only used by batch jobs that
	 * generates notifications
	 */
	public List<OrgUnit> getAllActiveWithAffiliations() {
		return orgUnitDao.findAllActiveWithAffiliations(organisationService.getAdmOrg().getId());
	}

	/**
	 * Never affected by future organisation changes, as it is only used by the SMS Module, and
	 * that works on NOW, and not potential future changes
	 */
	public List<OUAddressTreeNode> getAllTreeByAddress() {
		return getAllTreeByAddress(organisationService.getAdmOrg());
	}

	private List<OUAddressTreeNode> getAllTreeByAddress(Organisation organisation) {
		@SuppressWarnings("deprecation")
		List<OUAddressRow> result = jdbcTemplate.query(SELECT_THIN_ORGUNITS_WITH_ADDRESS_SQL, new Object[] { organisation.getId()}, (RowMapper<OUAddressRow>) (rs, rownum) -> {
			OUAddressRow ou = new OUAddressRow();

			ou.setId(rs.getString("uuid"));
			ou.setName(rs.getString("name"));
			ou.setCity(rs.getString("city"));
			ou.setAddress(rs.getString("address"));

			return ou;
		});

		Map<String, List<OUAddressRow>> groupedByPostCode = result.stream().collect(Collectors.groupingBy(OUAddressRow::getCity));

		List<OUAddressTreeNode> rootLevel = new ArrayList<OUAddressTreeNode>();

		for (Entry<String, List<OUAddressRow>> pair : groupedByPostCode.entrySet()) {
			OUAddressTreeNode firstNode = new OUAddressTreeNode();
			firstNode.setText(pair.getKey());
			firstNode.setChildren(new ArrayList<OUAddressTreeNode>());

			Map<String, List<OUAddressRow>> groupedByAddress = pair.getValue().stream().collect(Collectors.groupingBy(OUAddressRow::getAddress));
			for (Entry<String, List<OUAddressRow>> entry : groupedByAddress.entrySet()) {
				OUAddressTreeNode secondNode = new OUAddressTreeNode();
				secondNode.setText(entry.getKey());
				secondNode.setChildren(new ArrayList<OUAddressTreeNode>());

				for (OUAddressRow ou : entry.getValue()) {
					OUAddressTreeNode thirdNode = new OUAddressTreeNode();
					thirdNode.setId(ou.getId());
					thirdNode.setText(ou.getName());
					secondNode.getChildren().add(thirdNode);
				}

				firstNode.getChildren().add(secondNode);
			}

			rootLevel.add(firstNode);
		}

		return rootLevel;
	}

	@Transactional
	public OrgUnit save(OrgUnit orgUnit) throws Exception {
		Date date = getFutureDateFromSession();
		if (date != null) {
			// when operating in the future only these core fields can be edited
			OrgUnitCoreInfo coreInfo = new OrgUnitCoreInfo();
			coreInfo.setSourceName(orgUnit.getSourceName());
			coreInfo.setShortname(orgUnit.getShortname());
			coreInfo.setParent(orgUnit.getParent().getUuid());
			coreInfo.setBelongsTo(orgUnit.getBelongsTo().getId());
			coreInfo.setCostBearer(orgUnit.getCostBearer());
			coreInfo.setCvr(orgUnit.getCvr());
			coreInfo.setPnr(orgUnit.getPnr());
			coreInfo.setSenr(orgUnit.getSenr());
			coreInfo.setOrgUnitType(orgUnit.getOrgType());
			coreInfo.setDisplayName(orgUnit.getDisplayName());
			coreInfo.setManager(orgUnit.getSelectedManagerUuid());
			coreInfo.setOrgUnitType(orgUnit.getOrgType());
			coreInfo.setTags(orgUnit.getTags());

			return orgUnitFutureChangesService.saveFutureChanges(orgUnit.getUuid(), coreInfo, date);
		}

		return orgUnitDao.save(orgUnit);
	}

	// Should only be used by our BootstrapDevMode class, as it bypasses all interceptors
	@Deprecated
	public List<OrgUnit> saveAll(List<OrgUnit> orgUnits) throws Exception {
		return orgUnitDao.saveAll(orgUnits);
	}

	// note that this method will filter out positionNames if the affiliation master is OS2vikar
	// also note that this method is intended for IdM processes, and should not really be used elsewhere
	public Set<String> getPositionNames(OrgUnit ou, boolean onlyActive) {
		List<Affiliation> affiliations = ou.getAffiliations();

		// if we want more than just active affiliation positionNames, we include the last 6 months of
		// inactive affiliations as well
		var filteredAffiliations = onlyActive
				? AffiliationService.onlyActiveAffiliations(affiliations)
				: AffiliationService.allAffiliationsActiveSinceMonths(affiliations, 6);

		return filteredAffiliations.stream()
				.filter(a -> !Objects.equals(a.getMaster(), "OS2vikar"))
				.map(a -> AffiliationService.getPositionName(a))
				.collect(Collectors.toSet());
	}

	@Transactional
	public void updateCoreInformation(OrgUnit orgUnit, OrgUnitCoreInfo coreInfoDTO) throws Exception {
		Date date = getFutureDateFromSession();
		if (date != null) {
			orgUnitFutureChangesService.saveFutureChanges(orgUnit.getUuid(), coreInfoDTO, date);
			return;
		}

		boolean changes = false;
		boolean shouldUpdateChildren = false;

		List<OrgUnitType> types = getTypes();
		for (OrgUnitType type : types) {
			if (type.getKey().equals(coreInfoDTO.getOrgUnitType())) {
				if (!orgUnit.getType().getKey().equals(type.getKey())) {
					orgUnit.setType(type);
					changes = true;
				}
				break;
			}
		}

		// changing displayname is allowed even though los module is not enabled
		if (!Objects.equals(orgUnit.getDisplayName(), coreInfoDTO.getDisplayName())) {
			orgUnit.setDisplayName(coreInfoDTO.getDisplayName());
			changes = true;
		}

		// changing manager is allowed even though los module is not enabled
		log.trace("configuration.getModules().getManager().isEditEnabled(): " + configuration.getModules().getManager().isEditEnabled());
		if (configuration.getModules().getManager().isEditEnabled() && managerService.editSelectedManager(orgUnit, coreInfoDTO.getManager())) {
			changes = true;
			shouldUpdateChildren = true;
		}

		if (!Objects.equals(orgUnit.isDoNotTransferToFkOrg(), coreInfoDTO.isDoNotTransferToFKOrg())) {
			orgUnit.setDoNotTransferToFkOrg(coreInfoDTO.isDoNotTransferToFKOrg());
			changes = true;
			shouldUpdateChildren = true;
		}

		if (!Objects.equals(orgUnit.isBlockUpdate(), coreInfoDTO.isBlockUpdate())) {
			orgUnit.setBlockUpdate(coreInfoDTO.isBlockUpdate());
			changes = true;
		}


		if (SecurityUtil.getUserRoles().contains(RoleConstants.USER_ROLE_LOS_ADMIN)) {
			if (configuration.getModules().getLos().isEnabled()) {

				if (!Objects.equals(orgUnit.getSourceName(), coreInfoDTO.getSourceName())) {
					orgUnit.setSourceName(coreInfoDTO.getSourceName());
					changes = true;
				}

				if (!Objects.equals(orgUnit.getShortname(), coreInfoDTO.getShortname())) {
					orgUnit.setShortname(coreInfoDTO.getShortname());
					changes = true;
				}

				if (!Objects.equals(orgUnit.getCvr(), coreInfoDTO.getCvr())) {
					orgUnit.setCvr(coreInfoDTO.getCvr());
					changes = true;
				}

				if (!Objects.equals(orgUnit.getSenr(), coreInfoDTO.getSenr())) {
					orgUnit.setSenr(coreInfoDTO.getSenr());
					changes = true;
				}

				if (configuration.getIntegrations().getCvr().isEnabled()) {
					if (!Objects.equals(orgUnit.getPnr(), coreInfoDTO.getPnr())) {
						orgUnit.setPnr(coreInfoDTO.getPnr());
						changes = true;
						boolean createAndAddPost = false;

						if (!orgUnit.getPostAddresses().isEmpty()) {
							List<OrgUnitPostMapping> postsWithMasterCVR = orgUnit.getPostAddresses().stream().filter(p -> p.getPost().getMaster().equals("CVR")).collect(Collectors.toList());

							if (postsWithMasterCVR.isEmpty()) {
								createAndAddPost = true;
							}
							else {
								List<OrgUnitPostMapping> postsToDelete = postsWithMasterCVR.stream().filter(p -> !p.getPost().getMasterId().equals(coreInfoDTO.getPnr().toString())).collect(Collectors.toList());

								if (!postsToDelete.isEmpty()) {
									orgUnit.getPostAddresses().removeAll(postsToDelete);
									createAndAddPost = true;
								}
							}
						}

						if (createAndAddPost) {
							Post post = new Post();
							List<Post> postsWithPrime = getPosts(orgUnit).stream().filter(p -> p.isPrime()).collect(Collectors.toList());
							if (!postsWithPrime.isEmpty()) {
								post.setPrime(true);
							}
							else {
								post.setPrime(false);
							}

							post.setStreet(coreInfoDTO.getStreet());
							post.setPostalCode(coreInfoDTO.getPostalCode());
							post.setCity(coreInfoDTO.getCity());
							post.setCountry("Danmark");
							post.setAddressProtected(false);
							post.setMaster("CVR");
							post.setMasterId(coreInfoDTO.getPnr().toString());

							OrgUnitPostMapping mapping = new OrgUnitPostMapping();
							mapping.setOrgUnit(orgUnit);
							mapping.setPost(post);

							orgUnit.getPostAddresses().add(mapping);
						}
					}
					else {
						// there should only be one, but finds list to be sure
						List<Post> postsToUpdate = getPosts(orgUnit).stream().filter(p -> p.getMaster().equals("CVR") && p.getMasterId().equals(coreInfoDTO.getPnr().toString())).collect(Collectors.toList());
						if (orgUnit.getPnr() != null) {
							PUnitDTO dto = cvrService.getPUnitByPnr(orgUnit.getPnr().toString());
							if (dto != null) {
								for (Post post : postsToUpdate) {
									String street = dto.getStreet() + " " + dto.getNumber();

									if (!Objects.equals(post.getStreet(), street)) {
										post.setStreet(street);
										changes = true;
									}

									if (!Objects.equals(post.getPostalCode(), dto.getPostalCode())) {
										post.setPostalCode(dto.getPostalCode());
										changes = true;
									}

									if (!Objects.equals(post.getCity(), dto.getCity())) {
										post.setCity(dto.getCity());
										changes = true;
									}
								}
							}
						}
					}
				}
				else {
					if (!Objects.equals(orgUnit.getPnr(), coreInfoDTO.getPnr())) {
						orgUnit.setPnr(coreInfoDTO.getPnr());
						changes = true;
					}
				}

				if (!Objects.equals(orgUnit.getCostBearer(), coreInfoDTO.getCostBearer())) {
					orgUnit.setCostBearer(coreInfoDTO.getCostBearer());
					changes = true;
				}

				if (orgUnit.getParent() == null && StringUtils.hasLength(coreInfoDTO.getParent())) {
					log.warn("Tried to move a root OrgUnit.");
				}
				else if (StringUtils.hasLength(coreInfoDTO.getParent())) {
					OrgUnit newParent = getByUuid(coreInfoDTO.getParent());
					if (newParent == null) {
						log.warn("Unable to find OrgUnit with UUID: " + coreInfoDTO.getParent());
					}
					else if (orgUnit.getUuid().equals(newParent.getUuid())) {
						log.warn("Tried to set OU: " + orgUnit.getUuid() + " to be its own parent.");
					}
					else {
						if (checkIfIsAChild(orgUnit, newParent)) {
							log.warn("Cannot move parent into one of child units");
						}
						else {
							if (!orgUnit.getParent().getUuid().equals(newParent.getUuid())) {
								orgUnit.setParent(newParent);
								changes = true;
							}
						}
					}
				}
			}
		}


		if (changes) {
			// use autowired instance to ensure interceptors are called
			log.trace("Saving orgunit " + orgUnit.getUuid() + ", selectedManager: " + orgUnit.getSelectedManagerUuid() + ", importedManager: " + orgUnit.getImportedManagerUuid());
			self.save(orgUnit);

			// if changes was made to the fk org transfer setting we also need to update children to respect inheritance
			if (shouldUpdateChildren) {
				self.forceUpdateChildren(orgUnit);
			}
		}
	}

	private boolean checkIfIsAChild(OrgUnit orgUnit, OrgUnit newParent) {
		return orgUnit.getChildren().stream().anyMatch(o -> o.equals(newParent) || checkIfIsAChild(o, newParent));
	}

	public List<OUTreeForm> getOuTreeWithoutChildren(OrgUnit orgUnit) {
		// Get all OU
		List<OUTreeForm> orgUnits = getAllTree();

		// Get list of children and children's children of current OU and remove them
		List<String> toBeRemoved = removeChildren(orgUnits, orgUnit.getUuid());
		orgUnits.removeIf(ou -> toBeRemoved.contains(ou.getId()));

		// Remove selected ou from list
		orgUnits.removeIf(ou -> ou.getId().equals(orgUnit.getUuid()));

		return orgUnits;
	}

	private List<String> removeChildren(List<OUTreeForm> orgUnits, String parentUuid) {
		List<String> toBeRemoved = new ArrayList<String>();

		for (OUTreeForm node : orgUnits) {
			if (node.getParent().equals(parentUuid)) {
				toBeRemoved.add(node.getId());
				toBeRemoved.addAll(removeChildren(orgUnits, node.getId()));
			}
		}

		return toBeRemoved;
	}

	public List<OrgUnitType> getTypes() {
		return orgUnitTypeDao.findByActiveTrue();
	}

	public OrgUnitType findTypeById(long id) {
		return orgUnitTypeDao.findById(id);
	}

	public OrgUnitType saveType(OrgUnitType type) {
		return orgUnitTypeDao.save(type);
	}

	public OrgUnitType getDepartmentType() {
		Optional<OrgUnitType> type = getTypes().stream().filter(t -> "AFDELING".equals(t.getKey())).findFirst();
		if (!type.isPresent()) {
			throw new RuntimeException("AFDELING not present as team");
		}

		return type.get();
	}

	public OrgUnitType findTypeByKey(String key) {
		return orgUnitTypeDao.findByKey(key);
	}

	private static Date getFutureDateFromSession() {
		try {
			ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
			HttpSession session = attr.getRequest().getSession(true);

			return (Date) session.getAttribute(SessionConstants.SESSION_FUTURE_DATE);
		}
		catch (IllegalStateException ex) {
			// scheduled tasks will trigger this (no session on scheduled tasks ;o))
			if (!ex.getMessage().contains("No thread-bound request found")) {
				log.warn("Unable to extract future date from session", ex);
			}

			return null;
		}
	}

	// For getting the current object as it is in the database for use with OrgUnitFutureChangesService

	public OrgUnit getCurrentByUuid(String uuid) {
		return orgUnitDao.findByUuid(uuid);
	}

	public List<OrgUnit> getCurrentByUuid(List<String> uuids) {
		return orgUnitDao.findByUuidIn(uuids);
	}

	public List<OrgUnit> getCurrentAll(Organisation organisation) {
		return orgUnitDao.findByBelongsTo(organisation);
	}

	public List<OrgUnit> getCurrentAllActive(Organisation organisation) {
		return orgUnitDao.findByDeletedFalseAndBelongsTo(organisation);
	}

	public OrgUnit getByMasterId(String masterId) {
		return orgUnitDao.findByMasterId(masterId);
	}

	public static List<String> getKlePrimary(OrgUnit orgUnit) {
		if (orgUnit.getKlePrimary() == null) {
			return new ArrayList<>();
		}

		return orgUnit.getKlePrimary().stream().map(k -> k.getKleValue()).collect(Collectors.toList());
	}

	public static List<String> getKleSecondary(OrgUnit orgUnit) {
		if (orgUnit.getKleSecondary() == null) {
			return new ArrayList<>();
		}

		return orgUnit.getKleSecondary().stream().map(k -> k.getKleValue()).collect(Collectors.toList());
	}

	public static List<String> getKleTertiary(OrgUnit orgUnit) {
		if (orgUnit.getKleTertiary() == null) {
			return new ArrayList<>();
		}

		return orgUnit.getKleTertiary().stream().map(k -> k.getKleValue()).collect(Collectors.toList());
	}

	public static List<Phone> getPhones(OrgUnit orgUnit) {
		if (orgUnit.getPhones() == null) {
			return new ArrayList<>();
		}

		return orgUnit.getPhones().stream().map(p -> p.getPhone()).collect(Collectors.toList());
	}

	public static List<Post> getPosts(OrgUnit orgUnit) {
		if (orgUnit.getPostAddresses() == null) {
			return new ArrayList<>();
		}

		return orgUnit.getPostAddresses().stream().map(p -> p.getPost()).collect(Collectors.toList());
	}

	public List<OrgUnit> getAllWhereManagerIs(Person manager) {
		return orgUnitDao.findByManagerManager(manager);
	}

	public List<OrgUnit> getAllWhereIndirectOrDirectManagerIs(Person manager) {
		Set<OrgUnit> result = new HashSet<>();
		for (OrgUnit orgUnit : orgUnitDao.findByManagerManager(manager)) {
			result.add(orgUnit);
			result.addAll(orgUnit.getChildren());
		}

		return new ArrayList<>(result);
	}

	public List<OrgUnit> getByOrgUnitType(OrgUnitType type) {
		return orgUnitDao.findByType(type);
	}

	public List<OrgUnit> getByOrgUnitTypeId(Long orgUnitTypeId) {
		return orgUnitDao.findByOrgTypeId(orgUnitTypeId);
	}

	public void delete(OrgUnit orgUnit) {
		ModificationHistory modificationHistory = new ModificationHistory();
		modificationHistory.setEntity(EntityType.ORGUNIT);
		modificationHistory.setUuid(orgUnit.getUuid());
		modificationHistory.setChanged(new Date());
		modificationHistory.setChangeType(ChangeType.DELETE);

		modificationHistoryService.insert(modificationHistory);

		orgUnitDao.delete(orgUnit);
	}

	public boolean isDeletable(OrgUnit orgUnit) {

		if (orgUnit.isDeleted()) {
			return false;
		}

		// deletion not allowed if SOFD is not master
		if (!orgUnit.getMaster().equalsIgnoreCase("SOFD")) {
			return false;
		}

		// deletion not allowed if it has any non-deleted children
		if( orgUnit.getChildren() != null ) {
			for (var child : orgUnit.getChildren()) {

				if (!child.isDeleted()) {
					return false;
				}
			}
		}

		// deletion not allowed if it has any active affiliations
		var activeAffiliations = affiliationService.findByCalculatedOrgUnitAndActive(orgUnit);

		if (activeAffiliations.size() > 0) {
			return false;
		}

		return true;
	}

	public void forceUpdateChildren(OrgUnit orgUnit) {
		if( orgUnit.getChildren() == null ) {
			return;
		}
		orgUnit.getChildren().stream().filter(o -> !o.isDeleted()).forEach(child -> {
			ModificationHistory modificationHistory = new ModificationHistory();
			modificationHistory.setEntity(EntityType.ORGUNIT);
			modificationHistory.setUuid(child.getUuid());
			modificationHistory.setChanged(new Date());
			modificationHistory.setChangeType(ChangeType.UPDATE);
			modificationHistoryService.insert(modificationHistory);
			forceUpdateChildren(child);
		});
	}

	public List<OrgUnit> searchOrgUnits(String query) {
		return orgUnitDao.searchOrgUnits(query);
	}

	public List<SubstituteOrgUnitAssignmentDTO> getOrgUnitSubstitutes(OrgUnit orgUnit) {
		var result = new ArrayList<SubstituteOrgUnitAssignmentDTO>();
		result.addAll(orgUnit.getSubstitutes().stream().map(SubstituteOrgUnitAssignmentDTO::new).toList());
		addInheritedSubstitutesRecursive(orgUnit.getParent(), result);
		return result;
	}

	private void addInheritedSubstitutesRecursive(OrgUnit parent, List<SubstituteOrgUnitAssignmentDTO> substitutes) {
		if (parent == null) {
			return;
		}

		for (var substituteOrgUnitAssignment : parent.getSubstitutes()) {
			if (substituteOrgUnitAssignment.getContext().isInheritOrgUnitAssignments()) {
				substitutes.add(new SubstituteOrgUnitAssignmentDTO(substituteOrgUnitAssignment, true, parent.getName()));
			}
		}
		addInheritedSubstitutesRecursive(parent.getParent(), substitutes);
	}

	public List<OrgUnit> getActiveWhereAncestorDeleted() {
		return orgUnitDao.getActiveWhereAncestorDeleted();
	}

	public List<OrgUnit> getByOffsetAndLimit(String offset, int size) {
		if (StringUtils.hasText(offset)) {
			return orgUnitDao.findLimitedWithOffset(size, offset);
		}

		return orgUnitDao.findLimited(size);
	}

	@SuppressWarnings("unchecked")
	public Map<String, String> getLocalExtensionMap(OrgUnit orgUnit) {
		if (!StringUtils.hasLength(orgUnit.getLocalExtensions())) {
			return new HashMap<>();
		}
		try {
			Map<String, String> map = mapper.readValue(orgUnit.getLocalExtensions(), Map.class);
			// return sorted
			return new TreeMap<>(map);
		}
		catch (Exception ex) {
			log.error("Failed to convert string to map", ex);
			return new HashMap<>();
		}
    }
}