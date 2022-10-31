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
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
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
import dk.digitalidentity.sofd.dao.model.Email;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.OrgUnitType;
import dk.digitalidentity.sofd.dao.model.Organisation;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.Phone;
import dk.digitalidentity.sofd.dao.model.Post;
import dk.digitalidentity.sofd.dao.model.mapping.OrgUnitPostMapping;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.model.KleAssignmentDto;
import dk.digitalidentity.sofd.service.model.KleAssignmentType;
import dk.digitalidentity.sofd.service.model.OUAddressRow;
import dk.digitalidentity.sofd.service.model.OUAddressTreeNode;
import dk.digitalidentity.sofd.service.model.OUTagRow;
import dk.digitalidentity.sofd.service.model.OUTreeForm;
import dk.digitalidentity.sofd.service.model.OUTreeFormWithTags;
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
	 * SELECT o.uuid, o.name AS name, o.parent_uuid, t.id AS tag_id, m.name AS manager 
	 *	FROM orgunits o 
	 *	LEFT JOIN orgunits_tags ot ON ot.orgunit_uuid = o.uuid
	 *	LEFT JOIN tags t ON ot.tag_id = t.id 
	 *	LEFT JOIN orgunits_manager m ON m.orgunit_uuid = o.uuid
	 *	WHERE o.deleted = 0 
	 *	AND o.belongs_to = ?;
	 */
	private static final String SELECT_THIN_ORGUNITS_SQL_WITH_TAGS_AND_MANAGER = "SELECT o.uuid, o.name AS name, o.parent_uuid, t.id AS tag_id, m.name AS manager FROM orgunits o LEFT JOIN orgunits_tags ot ON ot.orgunit_uuid = o.uuid LEFT JOIN tags t ON ot.tag_id = t.id LEFT JOIN orgunits_manager m ON m.orgunit_uuid = o.uuid WHERE o.deleted = 0 AND o.belongs_to = ?;";

	
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
	
	public OrgUnit getByUuid(String uuid) {
		Date date = getFutureDateFromSession();
		if (date != null) {
			return orgUnitFutureChangesService.getFutureOrgUnit(uuid, date);
		}

		return orgUnitDao.findByUuid(uuid);
	}
	
	public Page<OrgUnit> getAll(Pageable pageable) {
		return orgUnitDao.findAll(pageable);
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

		return orgUnitDao.findBySourceName(sourceName);
	}

	public List<OrgUnit> getAll() {
		return getAll(organisationService.getAdmOrg());
	}

	public List<OrgUnit> getAll(Organisation organisation) {
		Date date = getFutureDateFromSession();
		if (date != null) {
			return orgUnitFutureChangesService.getAllFutureOrgUnits(organisation, date);
		}

		return orgUnitDao.findByBelongsTo(organisation);
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
				
				if (ou.getTagId() != null) {
					newForm.getTagIds().add(ou.getTagId());
				}
				
				treeFormList.add(newForm);
			} else {
				if (ou.getTagId() != null) {
					match.getTagIds().add(ou.getTagId());
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
				codes = orgUnit.getKlePrimary().stream().map(k -> k.getKleValue()).collect(Collectors.toList());
				break;
			case SECONDARY:
				codes = orgUnit.getKleSecondary().stream().map(k -> k.getKleValue()).collect(Collectors.toList());
				break;
			case TERTIARY:
				codes = orgUnit.getKleTertiary().stream().map(k -> k.getKleValue()).collect(Collectors.toList());
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

	public OrgUnit save(OrgUnit orgUnit) throws Exception {
		Date date = getFutureDateFromSession();
		if (date != null) {
			// when operating in the future only these core fields can be edited
			OrgUnitCoreInfo coreInfo = new OrgUnitCoreInfo();
			coreInfo.setSourceName(orgUnit.getName());
			coreInfo.setShortname(orgUnit.getShortname());
			coreInfo.setParent(orgUnit.getParent().getUuid());
			coreInfo.setBelongsTo(orgUnit.getBelongsTo().getId());
			coreInfo.setCostBearer(orgUnit.getCostBearer());
			coreInfo.setCvr(orgUnit.getCvr());
			coreInfo.setPnr(orgUnit.getPnr());
			coreInfo.setEan(orgUnit.getEan());
			coreInfo.setSenr(orgUnit.getSenr());
			coreInfo.setOrgUnitType(orgUnit.getOrgType());
			coreInfo.setDisplayName(orgUnit.getDisplayName());

			return orgUnitFutureChangesService.saveFutureChanges(orgUnit.getUuid(), coreInfo, date);
		}

		return orgUnitDao.save(orgUnit);
	}

	public Set<String> getPositionNames(OrgUnit ou, boolean onlyActive, boolean potentialDisplayName) {
		if (onlyActive) {
			if (potentialDisplayName) {
				return AffiliationService.onlyActiveAffiliations(ou.getAffiliations()).stream()
						.map(a -> AffiliationService.getPositionName(a))
						.collect(Collectors.toSet());
			} else {
				return AffiliationService.onlyActiveAffiliations(ou.getAffiliations()).stream()
						.map(a -> a.getPositionName())
						.collect(Collectors.toSet());
			}
		}

		// if we want more than just active affiliation positionNames, we include the last 6 months of
		// inactive affiliations as well
		if (potentialDisplayName) {
			return AffiliationService.allAffiliationsActiveSinceMonths(ou.getAffiliations(), 6).stream()
					.map(a -> AffiliationService.getPositionName(a))
					.collect(Collectors.toSet());
		} else {
			return AffiliationService.allAffiliationsActiveSinceMonths(ou.getAffiliations(), 6).stream()
					.map(a -> a.getPositionName())
					.collect(Collectors.toSet());
		}
	}

	@Transactional
	public void updateCoreInformation(OrgUnit orgUnit, OrgUnitCoreInfo coreInfoDTO) throws Exception {
		Date date = getFutureDateFromSession();
		if (date != null) {
			orgUnitFutureChangesService.saveFutureChanges(orgUnit.getUuid(), coreInfoDTO, date);
			return;
		}

		boolean changes = false;

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

		if (SecurityUtil.getUserRoles().contains(RoleConstants.USER_ROLE_LOS_ADMIN)) {
			if( configuration.getModules().getLos().isEnabled() ) {
				if (!Objects.equals(orgUnit.getSourceName(), coreInfoDTO.getSourceName())) {
					orgUnit.setSourceName(coreInfoDTO.getSourceName());
					changes = true;
				}

				if (!Objects.equals(orgUnit.getShortname(), coreInfoDTO.getShortname())) {
					orgUnit.setShortname(coreInfoDTO.getShortname());
					changes = true;
				}

				if (!Objects.equals(orgUnit.getEan(), coreInfoDTO.getEan())) {
					orgUnit.setEan(coreInfoDTO.getEan());
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

				if (configuration.getModules().getManager().isEditEnabled() && managerService.editManager(orgUnit, coreInfoDTO.getManager())) {
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
			self.save(orgUnit);
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
	
	public static List<Email> getEmails(OrgUnit orgUnit) {
		if (orgUnit.getEmails() == null) {
			return new ArrayList<>();
		}
		
		return orgUnit.getEmails().stream().map(e -> e.getEmail()).collect(Collectors.toList());
	}
	
	public static List<Post> getPosts(OrgUnit orgUnit) {
		if (orgUnit.getPostAddresses() == null) {
			return new ArrayList<>();
		}
		
		return orgUnit.getPostAddresses().stream().map(p -> p.getPost()).collect(Collectors.toList());
	}
	
	@Transactional
	public void cvrMaintenance() throws Exception {
		SecurityUtil.fakeLoginSession();
		Map<String, PUnitDTO> cvrMap = new HashMap<String, PUnitDTO>();
		for (OrgUnit orgUnit : orgUnitDao.findAll()) {
			boolean changes = false;
			List<Post> postsWithMasterCvr = getPosts(orgUnit).stream().filter(p -> p.getMaster().equals("CVR")).collect(Collectors.toList());
			// add a cvr post if one doesn't exist
			if( orgUnit.getPnr() != null && postsWithMasterCvr.size() == 0 )
			{
				PUnitDTO pUnitDTO = cvrMap.get(orgUnit.getPnr().toString());
				if (pUnitDTO == null) {
					pUnitDTO = cvrService.getPUnitByPnr(orgUnit.getPnr().toString());
					if( pUnitDTO != null)
					{
						cvrMap.put(orgUnit.getPnr().toString(), pUnitDTO);
					}
				}
				if (pUnitDTO != null) {
					Post post = new Post();
					post.setMaster("CVR");
					post.setMasterId(orgUnit.getPnr().toString());
					post.setCountry("Danmark");
					String street = pUnitDTO.getStreet() + " " + pUnitDTO.getNumber();
					post.setStreet(street);
					post.setPostalCode(pUnitDTO.getPostalCode());
					post.setCity(pUnitDTO.getCity());
					post.setPrime(orgUnit.getPostAddresses().isEmpty());
					OrgUnitPostMapping postMapping = new OrgUnitPostMapping();
					postMapping.setOrgUnit(orgUnit);
					postMapping.setPost(post);
					orgUnit.getPostAddresses().add(postMapping);
					changes = true;
				}
			}
			// update existing cvr posts
			for (Post post : postsWithMasterCvr) {
				PUnitDTO pUnitDTO = cvrMap.get(post.getMasterId());
				if(pUnitDTO == null)
				{
					pUnitDTO = cvrService.getPUnitByPnr(post.getMasterId());
					cvrMap.put(post.getMasterId(),pUnitDTO);
				}
				if( pUnitDTO != null)
				{
					String street = pUnitDTO.getStreet() + " " + pUnitDTO.getNumber();
					if (!Objects.equals(post.getStreet(), street)) {
						post.setStreet(street);
						changes = true;
					}
					if (!Objects.equals(post.getPostalCode(), pUnitDTO.getPostalCode())) {
						post.setPostalCode(pUnitDTO.getPostalCode());
						changes = true;
					}
					if (!Objects.equals(post.getCity(), pUnitDTO.getCity())) {
						post.setCity(pUnitDTO.getCity());
						changes = true;
					}
				}
			}
			if( orgUnit.getPnr() != null ) {
				PUnitDTO pUnitDTO = cvrMap.get(orgUnit.getPnr().toString());
				if (pUnitDTO == null) {
					pUnitDTO = cvrService.getPUnitByPnr(orgUnit.getPnr().toString());
					if (pUnitDTO != null) {
						cvrMap.put(orgUnit.getPnr().toString(), pUnitDTO);
					}
				}
				if (pUnitDTO != null) {
					// update cvr name
					// it is on purpose we only update if we get a non-null response from cvrService (to prevent nulling cvrName field if service is down or fails).
					if (!Objects.equals(orgUnit.getCvrName(), pUnitDTO.getName())) {
						orgUnit.setCvrName(pUnitDTO.getName());
						changes = true;
					}
				}
			}

			if (changes) {
				orgUnitDao.save(orgUnit);
			}
		}
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
}
