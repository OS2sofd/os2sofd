package dk.digitalidentity.sofd.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import dk.digitalidentity.sofd.dao.model.OrgUnitTag;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.digitalidentity.sofd.controller.rest.model.OrgUnitCoreInfo;
import dk.digitalidentity.sofd.dao.OrgUnitFutureChangesDao;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.OrgUnitFutureChange;
import dk.digitalidentity.sofd.dao.model.OrgUnitManager;
import dk.digitalidentity.sofd.dao.model.OrgUnitType;
import dk.digitalidentity.sofd.dao.model.Organisation;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.enums.AppliedStatus;
import dk.digitalidentity.sofd.dao.model.enums.OrgUnitAttribute;
import dk.digitalidentity.sofd.dao.model.enums.OrgUnitChangeType;
import dk.digitalidentity.sofd.service.model.OUTreeForm;
import dk.digitalidentity.sofd.service.model.OUTreeFormWithTags;
import lombok.extern.slf4j.Slf4j;

// TODO: consider moving a lot of this "apply" logic into SofdRepositoryImpl class
@Slf4j
@Service
public class OrgUnitFutureChangesService {

	@Autowired
	private OrgUnitService orgUnitService;

	@Autowired
	private PersonService personService;

	@Autowired
	private OrgUnitFutureChangesDao futureChangesDao;

	@Autowired
	private OrganisationService organisationService;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private OrgUnitFutureChangesService self;

	@Autowired
	private TagsService tagsService;

	public List<OUTreeForm> getAllTreeFutureOrgUnits(List<OUTreeForm> ouTreeForms, Date date) {
		List<OrgUnitFutureChange> oUsChangesTillDate = getAllChangesTillDateAndNotApplied(date);

		List<OrgUnitFutureChange> changes = oUsChangesTillDate.stream()
				.filter(change -> !change.getChangeType().equals(OrgUnitChangeType.UPDATE_ATTRIBUTE)
						|| (change.getChangeType().equals(OrgUnitChangeType.UPDATE_ATTRIBUTE) && change.getAttributeField().equals(OrgUnitAttribute.NAME)))
				.sorted(getSorter()).collect(Collectors.toList());

		return applyChangesOUTreeForm(ouTreeForms, changes);
	}
	
	public List<OUTreeFormWithTags> getAllTreeWithTagsFutureOrgUnits(List<OUTreeFormWithTags> ouTreeForms, Date date) {
		List<OrgUnitFutureChange> oUsChangesTillDate = getAllChangesTillDateAndNotApplied(date);

		List<OrgUnitFutureChange> changes = oUsChangesTillDate.stream()
				.filter(change -> !change.getChangeType().equals(OrgUnitChangeType.UPDATE_ATTRIBUTE)
						|| (change.getChangeType().equals(OrgUnitChangeType.UPDATE_ATTRIBUTE) && change.getAttributeField().equals(OrgUnitAttribute.NAME)))
				.sorted(getSorter()).collect(Collectors.toList());

		return applyChangesOUTreeWithTagsForm(ouTreeForms, changes);
	}

	public OrgUnitFutureChange getById(long id) {
		return futureChangesDao.findById(id);
	}

	public List<OrgUnitFutureChange> getAllNotApplied() {
		List<OrgUnitFutureChange> changes = futureChangesDao.findAllByAppliedStatus(AppliedStatus.NOT_APPLIED);
		changes.sort(getSorter());

		return changes;
	}

	public List<OrgUnitFutureChange> getAllByOrgUnitAndNotApplied(OrgUnit orgUnit) {
		List<OrgUnitFutureChange> changes = futureChangesDao.findAllByOrgunitUuidAndAppliedStatus(orgUnit.getUuid(), AppliedStatus.NOT_APPLIED);
		changes.sort(getSorter());

		return changes;
	}

	public List<OrgUnitFutureChange> getOUChangesTillDateAndNotApplied(String uuid, Date date) {
		List<OrgUnitFutureChange> changes = futureChangesDao.findAllByOrgunitUuidAndChangeDateLessThanEqualAndAppliedStatus(uuid, date, AppliedStatus.NOT_APPLIED);
		changes.sort(getSorter());

		return changes;
	}

	public List<OrgUnitFutureChange> getOUsChangesTillDateAndNotApplied(List<String> uuids, Date date) {
		List<OrgUnitFutureChange> changes = futureChangesDao.findAllByOrgunitUuidInAndChangeDateLessThanEqualAndAppliedStatus(uuids, date, AppliedStatus.NOT_APPLIED);
		changes.sort(getSorter());

		return changes;
	}

	public List<OrgUnitFutureChange> getAllChangesTillDateAndNotApplied(Date date) {
		List<OrgUnitFutureChange> changes = futureChangesDao.findAllByChangeDateLessThanEqualAndAppliedStatus(date, AppliedStatus.NOT_APPLIED);
		changes.sort(getSorter());

		return changes;
	}

	public void delete(OrgUnitFutureChange orgUnitFutureChange) {
		futureChangesDao.delete(orgUnitFutureChange);
	}

	public OrgUnit getFutureOrgUnit(String uuid, Date date) {
		OrgUnit existingOU = orgUnitService.getCurrentByUuid(uuid);

		if (existingOU != null) {
			loadFullObject(existingOU);
			entityManager.detach(existingOU);
		}

		// Apply changes to detached object
		List<OrgUnitFutureChange> changes = getOUChangesTillDateAndNotApplied(uuid, date);
		
		return applyChanges(existingOU, changes);
	}

	public List<OrgUnit> getFutureOrgUnits(List<String> uuids, Date date) {
		List<OrgUnit> existingOUs = orgUnitService.getCurrentByUuid(uuids);

		fullLoadAndDetach(existingOUs);

		// Apply changes to detached object
		List<OrgUnitFutureChange> changes = getOUsChangesTillDateAndNotApplied(uuids, date);
		
		return applyChanges(existingOUs, changes);
	}

	public List<OrgUnit> getAllFutureOrgUnits(Organisation organisation, Date date) {
		List<OrgUnit> existingOUs = orgUnitService.getCurrentAll(organisation);
		List<String> uuids = existingOUs.stream().map(OrgUnit::getUuid).collect(Collectors.toList());

		fullLoadAndDetach(existingOUs);

		// Apply changes to detached object
		List<OrgUnitFutureChange> changes = getOUsChangesTillDateAndNotApplied(uuids, date);
		
		return applyChanges(existingOUs, changes);
	}

	public List<OrgUnit> getAllActiveFutureOrgUnits(Organisation organisation, Date date) {
		List<OrgUnit> existingOUs = orgUnitService.getCurrentAllActive(organisation);
		List<String> uuids = existingOUs.stream().map(OrgUnit::getUuid).collect(Collectors.toList());

		fullLoadAndDetach(existingOUs);

		// Apply changes to detached object
		List<OrgUnitFutureChange> changes = getOUsChangesTillDateAndNotApplied(uuids, date);

		return applyChanges(existingOUs, changes);
	}

	public OrgUnit saveFutureChanges(String uuid, OrgUnitCoreInfo orgUnitCoreInfo, Date date) throws Exception {
		OrgUnit orgUnit = getFutureOrgUnit(uuid, date);

		// TODO: delete case not handled (yet)
		
		if (orgUnit == null) {
			return createFutureOrgUnit(uuid, orgUnitCoreInfo, date);
		}
		else {
			return updateFutureOrgUnit(orgUnit, orgUnitCoreInfo, date);
		}
	}

	public void mergeFutureChanges() {
		if (countByAppliedStatus(AppliedStatus.ERROR) > 0) {
			log.error("Cannot apply changes because there are errors");
			return;
		}

		Date today = new Date();
		var changes = getAllChangesTillDateAndNotApplied(today);
		for (OrgUnitFutureChange change : changes) {
			var success = self.mergeFutureChange(change);
			if( !success ) {
				// stop doing more changes
				break;
			}
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public boolean mergeFutureChange(OrgUnitFutureChange change) {
		var today = new Date();
		try {
			var changedOU = getFutureOrgUnit(change.getOrgunitUuid(), today);
			entityManager.merge(changedOU);
			change.setAppliedStatus(AppliedStatus.APPLIED);
			change.setAppliedDate(today);
			save(change);
			entityManager.flush();
			return true;
		}
		catch (Exception ex) {
			change.setAppliedStatus(AppliedStatus.ERROR);
			change.setAppliedDate(today);
			save(change);
			log.error("Failed to apply futureChange", ex);
			return false;
		}
	}

	private void save(OrgUnitFutureChange orgUnitFutureChange) {
		futureChangesDao.save(orgUnitFutureChange);
	}

	private OrgUnit updateFutureOrgUnit(OrgUnit orgUnit, OrgUnitCoreInfo orgUnitCoreInfo, Date date) {
		boolean valid = true;
		String name = orgUnit.getName();
		String uuid = orgUnit.getUuid();

		List<OrgUnitFutureChange> newChanges = new ArrayList<>();
		for (OrgUnitAttribute attribute : OrgUnitAttribute.values()) {
			OrgUnitFutureChange newChange = null;

			switch (attribute) {
				case BELONGS_TO:
					; // it should NEVER be possible to move to another Organisation
					break;
				case NAME:
					if (!java.util.Objects.equals(orgUnit.getSourceName(), orgUnitCoreInfo.getSourceName())) {
						newChange = new OrgUnitFutureChange(uuid, name, OrgUnitAttribute.NAME, orgUnitCoreInfo.getSourceName(), date);
					}
					break;
				case SHORT_NAME:
					if (!java.util.Objects.equals(orgUnit.getShortname(), orgUnitCoreInfo.getShortname())) {
						newChange = new OrgUnitFutureChange(uuid, name, OrgUnitAttribute.SHORT_NAME, orgUnitCoreInfo.getShortname(), date);
					}
					break;
				case TYPE:
					OrgUnitType type = orgUnitService.findTypeByKey(orgUnitCoreInfo.getOrgUnitType());
					if (!java.util.Objects.equals(orgUnit.getOrgType(), type)) {
						newChange = new OrgUnitFutureChange(uuid, name, OrgUnitAttribute.TYPE, orgUnitCoreInfo.getOrgUnitType(), date);
					}
					break;
				case CVR:
					if (orgUnitCoreInfo.getCvr() != null && !java.util.Objects.equals(orgUnit.getCvr(), orgUnitCoreInfo.getCvr())) {
						newChange = new OrgUnitFutureChange(uuid, name, OrgUnitAttribute.CVR, Long.toString(orgUnitCoreInfo.getCvr()), date);
					}
					break;
				case SENR:
					if (orgUnitCoreInfo.getSenr() != null && !java.util.Objects.equals(orgUnit.getSenr(), orgUnitCoreInfo.getSenr())) {
						newChange = new OrgUnitFutureChange(uuid, name, OrgUnitAttribute.SENR, Long.toString(orgUnitCoreInfo.getSenr()), date);
					}
					break;
				case PNR:
					if (orgUnitCoreInfo.getPnr() != null && !java.util.Objects.equals(orgUnit.getPnr(), orgUnitCoreInfo.getPnr())) {
						newChange = new OrgUnitFutureChange(uuid, name, OrgUnitAttribute.PNR, Long.toString(orgUnitCoreInfo.getPnr()), date);
					}
					break;
				case COST_BEARER:
					if (StringUtils.hasLength(orgUnitCoreInfo.getCostBearer()) && !java.util.Objects.equals(orgUnit.getCostBearer(), orgUnitCoreInfo.getCostBearer())) {
						newChange = new OrgUnitFutureChange(uuid, name, OrgUnitAttribute.COST_BEARER, orgUnitCoreInfo.getCostBearer(), date);
					}
					break;
				case PARENT:
					if ((StringUtils.hasLength(orgUnitCoreInfo.getParent()) && orgUnit.getParent() == null) ||
						(!StringUtils.hasLength(orgUnitCoreInfo.getParent()) && orgUnit.getParent() != null) ||
						(orgUnit.getParent() != null && !java.util.Objects.equals(orgUnit.getParent().getUuid(), orgUnitCoreInfo.getParent()))) {
						
						if (checkIfValidMove(uuid, orgUnitCoreInfo.getParent(), date)) {
							OrgUnit futureParent = getFutureOrgUnit(orgUnitCoreInfo.getParent(), date);
							newChange = new OrgUnitFutureChange(uuid, name, futureParent.getUuid(), futureParent.getName(), date);
						}
						else {
							// if it's not a valid move, throw out all the changes
							valid = false;
						}
					}
					break;
				case DISPLAY_NAME:
					if (StringUtils.hasLength(orgUnitCoreInfo.getDisplayName()) && !java.util.Objects.equals(orgUnit.getDisplayName(), orgUnitCoreInfo.getDisplayName())) {
						newChange = new OrgUnitFutureChange(uuid, name, OrgUnitAttribute.DISPLAY_NAME, orgUnitCoreInfo.getDisplayName(), date);
					}
					break;
				case MANAGER:
					if ((StringUtils.hasLength(orgUnitCoreInfo.getManager()) && orgUnit.getManager() == null) ||
						(!StringUtils.hasLength(orgUnitCoreInfo.getManager()) && orgUnit.getManager() != null) ||
						(orgUnit.getManager() != null && !java.util.Objects.equals(orgUnit.getManager().getManager().getUuid(), orgUnitCoreInfo.getManager()))) {
						
						newChange = new OrgUnitFutureChange(uuid, name, OrgUnitAttribute.MANAGER, orgUnitCoreInfo.getManager(), date);
					}
					break;
			default:
				break;
					
					// Do not make a default case, we will allow our IDE to help us add new attributes in the future
			}

			if (newChange != null) {
				newChanges.add(newChange);
			}
		}

		// handle tags
		for (var dtoTag :  orgUnitCoreInfo.getTags()) {
			var dbTag = orgUnit.getTags().stream().filter(t -> t.getTag().getId() == dtoTag.getTag().getId()).findFirst().orElse(null);
			if (dbTag != null) {
				// update - ignore. We can't change tags, only add or remove them. Tag custom values are only set on creation
			}
			else {
				// create tag
				var newChange = new OrgUnitFutureChange();
				newChange.setChangeDate(date);
				newChange.setChangeType(OrgUnitChangeType.ADD_TAG);
				newChange.setOrgunitUuid(uuid);
				newChange.setOrgunitName(name);
				newChange.setTagId(dtoTag.getTag().getId());
				newChange.setTagValue(dtoTag.getCustomValue());
				newChanges.add(newChange);
			}
		}
		for (var dbTag : orgUnit.getTags()) {
			var dtoTag = orgUnitCoreInfo.getTags().stream().filter(t -> t.getTag().getId() == dbTag.getTag().getId()).findFirst().orElse(null);
			if (dtoTag == null) {
				// delete tag
				var newChange = new OrgUnitFutureChange();
				newChange.setChangeDate(date);
				newChange.setChangeType(OrgUnitChangeType.REMOVE_TAG);
				newChange.setOrgunitUuid(uuid);
				newChange.setOrgunitName(name);
				newChange.setTagId(dbTag.getTag().getId());
				newChanges.add(newChange);
			}
		}

		if (valid) {
			for (OrgUnitFutureChange newChange : newChanges) {
				save(newChange);
			}

			return getFutureOrgUnit(uuid, date);
		}
		
		return null;
	}

	private OrgUnit createFutureOrgUnit(String uuid, OrgUnitCoreInfo orgUnitCoreInfo, Date date) throws Exception {
		Map<OrgUnitAttribute, String> map = new HashMap<>();

		for (OrgUnitAttribute attribute : OrgUnitAttribute.values()) {
			switch(attribute) {
				case BELONGS_TO:
					map.put(OrgUnitAttribute.BELONGS_TO, Long.toString(orgUnitCoreInfo.getBelongsTo()));
					break;
				case COST_BEARER:
					map.put(OrgUnitAttribute.COST_BEARER, orgUnitCoreInfo.getCostBearer());
					break;
				case CVR:
					map.put(OrgUnitAttribute.CVR, orgUnitCoreInfo.getCvr() != null ? Long.toString(orgUnitCoreInfo.getCvr()) : null);
					break;
				case NAME:
					map.put(OrgUnitAttribute.NAME, orgUnitCoreInfo.getSourceName());
					break;
				case PARENT:
					map.put(OrgUnitAttribute.PARENT, orgUnitCoreInfo.getParent());
					break;
				case PNR:
					map.put(OrgUnitAttribute.PNR, orgUnitCoreInfo.getPnr() != null ? Long.toString(orgUnitCoreInfo.getPnr()) : null);
					break;
				case SENR:
					map.put(OrgUnitAttribute.SENR, orgUnitCoreInfo.getSenr() != null ? Long.toString(orgUnitCoreInfo.getSenr()) : null);
					break;
				case SHORT_NAME:
					map.put(OrgUnitAttribute.SHORT_NAME, orgUnitCoreInfo.getShortname());
					break;
				case TYPE:
					map.put(OrgUnitAttribute.TYPE, orgUnitCoreInfo.getOrgUnitType());
					break;
				case DISPLAY_NAME:
					map.put(OrgUnitAttribute.DISPLAY_NAME, orgUnitCoreInfo.getDisplayName());
					break;
				case MANAGER:
					map.put(OrgUnitAttribute.MANAGER, orgUnitCoreInfo.getManager());
					break;
			default:
				break;
					
				// TODO: no default case so adding new fields will be automatically detected by IDE
			}
		}

		ObjectMapper mapper = new ObjectMapper();
		String createPayload = mapper.writeValueAsString(map);

		OrgUnit parentOU = getFutureOrgUnit(orgUnitCoreInfo.getParent(), date);
		
		String name = orgUnitCoreInfo.getDisplayName() != null && orgUnitCoreInfo.getDisplayName().isEmpty() ? orgUnitCoreInfo.getDisplayName() : orgUnitCoreInfo.getSourceName();
		
		if (parentOU != null) {
			save(new OrgUnitFutureChange(uuid, name, createPayload, date, parentOU.getUuid(), parentOU.getName(), orgUnitCoreInfo.getDisplayName()));
		}

		return getFutureOrgUnit(uuid, date);
	}

	private boolean checkIfValidMove(String orgUnitUuid, String newParentUuid, Date date) {
		if (orgUnitUuid.equals(newParentUuid)) {
			log.warn("Tried to set OU: " + orgUnitUuid + " to be its own parent at date: " + date.toString());
			return false;
		}

		OrgUnit newParentOU = orgUnitService.getByUuid(newParentUuid);
		if (newParentOU == null) {
			log.warn("Unable to find OrgUnit with UUID: " + newParentUuid + "at date: " + date.toString());
			return false;
		}

		OrgUnit orgUnit = orgUnitService.getByUuid(orgUnitUuid);
		if (checkIfNewParentIsChild(orgUnit, newParentUuid)) {
			log.warn("Cannot set OU (" + orgUnitUuid + ") to be a parent of one if its children (" + newParentUuid + ") at date: " + date.toString());
			return false;
		}
		return true;
	}

	private boolean checkIfNewParentIsChild(OrgUnit orgUnit, String newParent) {
		return CollectionUtils.emptyIfNull(orgUnit.getChildren()).stream().anyMatch(ou -> ou.getUuid().equals(newParent) || checkIfNewParentIsChild(ou, newParent));
	}

	private void fullLoadAndDetach(List<OrgUnit> orgUnits) {
		for (OrgUnit ou : orgUnits) {
			loadFullObject(ou);
			entityManager.detach(ou);
		}
	}

	private void loadFullObject(OrgUnit orgUnit) {
		orgUnit.getParent();
		orgUnit.getBelongsTo();
		orgUnit.getChildren().size();
		orgUnit.getPostAddresses().stream().forEach(m -> m.getPost().getCity());
		orgUnit.getPhones().stream().forEach(m -> m.getPhone().getPhoneNumber());
		orgUnit.getEanList().stream().forEach(m -> m.getMaster());
		orgUnit.getAffiliations().size();
		orgUnit.getTags().stream().forEach(m -> m.getTag().getValue());
		orgUnit.getSubstitutes().stream().forEach(m -> {
			m.getContext().getName();
			m.getId();
			m.getSubstitute().getFirstname();
			m.getSubstitute().getSurname();
			m.getSubstitute().getChosenName();
		});
	}

	private List<OrgUnit> applyChanges(List<OrgUnit> orgUnits, List<OrgUnitFutureChange> orgUnitFutureChanges) {
		Map<String, OrgUnit> map = orgUnits.stream().collect(Collectors.toMap(OrgUnit::getUuid, Function.identity()));

		for (OrgUnitFutureChange change : orgUnitFutureChanges) {
			OrgUnit orgUnit = map.get(change.getOrgunitUuid());
			map.put(change.getOrgunitUuid(), applyChange(orgUnit, change));
		}

		return new ArrayList<>(map.values());
	}

	private OrgUnit applyChanges(OrgUnit orgUnit, List<OrgUnitFutureChange> orgUnitFutureChanges) {
		for (OrgUnitFutureChange change : orgUnitFutureChanges) {
			orgUnit = applyChange(orgUnit, change);
		}

		return orgUnit;
	}

	private OrgUnit applyChange(OrgUnit orgUnit, OrgUnitFutureChange change) {
		if ((orgUnit == null && change.getChangeType() != OrgUnitChangeType.CREATE) || (change.getChangeType().equals(OrgUnitChangeType.CREATE) && !StringUtils.hasLength(change.getCreatePayload()))) {
			throw new RuntimeException();
		}

		switch (change.getChangeType()) {
			case CREATE:
				try {
					ObjectMapper mapper = new ObjectMapper();
					TypeReference<HashMap<OrgUnitAttribute, String>> typeRef = new TypeReference<HashMap<OrgUnitAttribute, String>>() { };
					Map<OrgUnitAttribute, String> coreInfoMap = mapper.readValue(change.getCreatePayload(), typeRef);

					orgUnit = new OrgUnit();
					orgUnit.setUuid(change.getOrgunitUuid());
					orgUnit.setSourceName(coreInfoMap.get(OrgUnitAttribute.NAME));
					orgUnit.setShortname(coreInfoMap.get(OrgUnitAttribute.SHORT_NAME));

					if (coreInfoMap.get(OrgUnitAttribute.CVR) != null) {
						orgUnit.setCvr(Long.parseLong(coreInfoMap.get(OrgUnitAttribute.CVR)));
					}
					
					if (coreInfoMap.get(OrgUnitAttribute.SENR) != null) {
						orgUnit.setSenr(Long.parseLong(coreInfoMap.get(OrgUnitAttribute.SENR)));
					}
					
					if (coreInfoMap.get(OrgUnitAttribute.PNR) != null) {
						orgUnit.setPnr(Long.parseLong(coreInfoMap.get(OrgUnitAttribute.PNR)));
					}
					
					orgUnit.setCostBearer(coreInfoMap.get(OrgUnitAttribute.COST_BEARER));
					orgUnit.setParent(getFutureOrgUnit(coreInfoMap.get(OrgUnitAttribute.PARENT), change.getChangeDate()));
					orgUnit.setBelongsTo(organisationService.getById(Long.parseLong(coreInfoMap.get(OrgUnitAttribute.BELONGS_TO))));
					orgUnit.setCreated(change.getChangeDate());
					orgUnit.setLastChanged(change.getChangeDate());
					orgUnit.setMaster("SOFD");
					orgUnit.setMasterId(UUID.randomUUID().toString());
					orgUnit.setType(orgUnitService.getDepartmentType());
					orgUnit.setDisplayName(change.getDisplayName());

					String futureManagerUuid = coreInfoMap.get(OrgUnitAttribute.MANAGER);
					if (StringUtils.hasLength(futureManagerUuid)) {
						Person futureManager = personService.getByUuid(futureManagerUuid);
						if (futureManager != null) {
							OrgUnitManager futureManagerMapping = new OrgUnitManager(orgUnit, futureManager, false);
							orgUnit.setManager(futureManagerMapping); // needed for display purposes in UI
							orgUnit.setSelectedManagerUuid(futureManager.getUuid()); // needed to update database
						} else {
							log.warn("Unable to find future Manager with uuid" + futureManagerUuid + " when applying changes to OrgUnit (CREATE scenario)");
						}
					}

					// collections needed for the UI has to be initialized
					orgUnit.setTags(new ArrayList<>());
					orgUnit.setAffiliations(new ArrayList<>());
				}
				catch (IOException ex) {
					log.error("Failed to handle CREATE case", ex);
				}

				break;
			case UPDATE_ATTRIBUTE:
				switch (change.getAttributeField()) {
					case PARENT:
						// handled in "MOVE" case
						break;
					case BELONGS_TO:
						// never possible
						break;
					case NAME:
						orgUnit.setSourceName(change.getAttributeValue());
						break;
					case SHORT_NAME:
						orgUnit.setShortname(change.getAttributeValue());
						break;
					case TYPE:
						OrgUnitType type = orgUnitService.findTypeByKey(change.getAttributeValue());
						if (type != null) {
							orgUnit.setType(type);
						}
						else {
							log.error("Could not find OrgUnitType by key: " + change.getAttributeValue());
						}
						break;
					case CVR:
						orgUnit.setCvr(Long.parseLong(change.getAttributeValue()));
						break;
					case SENR:
						orgUnit.setSenr(Long.parseLong(change.getAttributeValue()));
						break;
					case PNR:
						orgUnit.setPnr(Long.parseLong(change.getAttributeValue()));
						break;
					case COST_BEARER:
						orgUnit.setCostBearer(change.getAttributeValue());
						break;
					case DISPLAY_NAME:
						orgUnit.setDisplayName(change.getAttributeValue());
						break;
					case MANAGER:
						Person futureManager = personService.getByUuid(change.getAttributeValue());
						if (futureManager != null) {
							OrgUnitManager futureManagerMapping = new OrgUnitManager(orgUnit, futureManager, false);
							orgUnit.setManager(futureManagerMapping); // needed for display purposes in UI
							orgUnit.setSelectedManagerUuid(futureManager.getUuid()); // needed to update database
						} else {
							log.warn("Unable to find future Manager with uuid" + change.getAttributeValue() + " when applying changes to OrgUnit (UPDATE scenario)");
						}
						break;
				default:
					break;
						

					// do not make a default case - if new attributes are added in the future, our IDE
					// will help us here, by giving a warning.
				}
				break;
			case MOVE:
				OrgUnit newParent = orgUnitService.getByUuid(change.getParentUuid());
				if (newParent == null) {
					throw new RuntimeException();
				}

				OrgUnit oldParent = orgUnit.getParent();
				if (oldParent != null) {
					oldParent.getChildren().remove(orgUnit);
				}

				orgUnit.setParent(newParent);
				newParent.getChildren().add(orgUnit);
				break;
			case ADD_TAG:
				var tag = tagsService.findById(change.getTagId());
				if (tag != null) {
					var tagAssignment = new OrgUnitTag();
					tagAssignment.setOrgUnit(orgUnit);
					tagAssignment.setTag(tag);
					tagAssignment.setCustomValue(change.getTagValue());
					orgUnit.getTags().removeIf(t -> t.getTag().getId() == tag.getId()); // remove previously added future tag changes
					orgUnit.getTags().add(tagAssignment);
				}
				else {
					log.warn("Unable to find tag with id" + change.getTagId() + " when applying changes to OrgUnit (UPDATE scenario)");
				}
				break;
			case REMOVE_TAG:
				orgUnit.getTags().removeIf(t -> t.getTag().getId() ==  change.getTagId());
				break;
			case DELETE:
				// TODO: once we allow deleting OrgUnits in the future, we should implement this,
				//       but right now there is no way to delete OrgUnits (neither now nor in the future),
				//       so we do not need to support it (yet)
				orgUnit = null;
				break;
		}

		return orgUnit;
	}

	private List<OUTreeForm> applyChangesOUTreeForm(List<OUTreeForm> ouTreeForms, List<OrgUnitFutureChange> orgUnitFutureChanges) {
		Map<String, OUTreeForm> map = ouTreeForms.stream().collect(Collectors.toMap(OUTreeForm::getId, Function.identity()));

		for (OrgUnitFutureChange change : orgUnitFutureChanges) {
			OUTreeForm ouTreeForm = map.get(change.getOrgunitUuid());
			map.put(change.getOrgunitUuid(), applyChangeOUTreeForm(ouTreeForm, change));
		}

		return new ArrayList<>(map.values());
	}
	
	private List<OUTreeFormWithTags> applyChangesOUTreeWithTagsForm(List<OUTreeFormWithTags> ouTreeForms, List<OrgUnitFutureChange> orgUnitFutureChanges) {
		Map<String, OUTreeFormWithTags> map = ouTreeForms.stream().collect(Collectors.toMap(OUTreeFormWithTags::getId, Function.identity()));

		for (OrgUnitFutureChange change : orgUnitFutureChanges) {
			OUTreeFormWithTags ouTreeForm = map.get(change.getOrgunitUuid());
			map.put(change.getOrgunitUuid(), applyChangeOUTreeWithTagsForm(ouTreeForm, change));
		}

		return new ArrayList<>(map.values());
	}

	private OUTreeForm applyChangeOUTreeForm(OUTreeForm ouTreeForm, OrgUnitFutureChange change) {
		switch (change.getChangeType()) {
			case CREATE:
				try {
					ObjectMapper mapper = new ObjectMapper();
					TypeReference<HashMap<OrgUnitAttribute, String>> typeRef = new TypeReference<HashMap<OrgUnitAttribute, String>>() {};
					Map<OrgUnitAttribute, String> coreInfoMap = mapper.readValue(change.getCreatePayload(), typeRef);

					ouTreeForm = new OUTreeForm();
					ouTreeForm.setId(change.getOrgunitUuid());
					ouTreeForm.setText(coreInfoMap.get(OrgUnitAttribute.NAME));

					String parent = coreInfoMap.get(OrgUnitAttribute.PARENT);
					ouTreeForm.setParent(!StringUtils.hasLength(parent) ? "#" : parent);
				}
				catch (IOException ex) {
					log.error("Failed to create future orgunit", ex);
				}
				break;
			case UPDATE_ATTRIBUTE:
				if (OrgUnitAttribute.NAME.equals(change.getAttributeField())) {
					ouTreeForm.setText(change.getAttributeValue());
				}
				break;
			case MOVE:
				OrgUnit parent = orgUnitService.getByUuid(change.getParentUuid());
				if (parent == null) {
					throw new RuntimeException();
				}

				ouTreeForm.setParent(change.getParentUuid());
				break;
			case DELETE:
				ouTreeForm = null;
				break;
		}
		
		return ouTreeForm;
	}
	
	private OUTreeFormWithTags applyChangeOUTreeWithTagsForm(OUTreeFormWithTags ouTreeForm, OrgUnitFutureChange change) {
		switch (change.getChangeType()) {
			case CREATE:
				try {
					ObjectMapper mapper = new ObjectMapper();
					TypeReference<HashMap<OrgUnitAttribute, String>> typeRef = new TypeReference<HashMap<OrgUnitAttribute, String>>() {};
					Map<OrgUnitAttribute, String> coreInfoMap = mapper.readValue(change.getCreatePayload(), typeRef);

					ouTreeForm = new OUTreeFormWithTags();
					ouTreeForm.setId(change.getOrgunitUuid());
					ouTreeForm.setText(coreInfoMap.get(OrgUnitAttribute.NAME));
					ouTreeForm.setTagIds(new ArrayList<Long>());

					String parent = coreInfoMap.get(OrgUnitAttribute.PARENT);
					ouTreeForm.setParent(!StringUtils.hasLength(parent) ? "#" : parent);
				}
				catch (IOException ex) {
					log.error("Failed to create future orgunit", ex);
				}
				break;
			case UPDATE_ATTRIBUTE:
				if (OrgUnitAttribute.NAME.equals(change.getAttributeField())) {
					ouTreeForm.setText(change.getAttributeValue());
				}
				break;
			case MOVE:
				OrgUnit parent = orgUnitService.getByUuid(change.getParentUuid());
				if (parent == null) {
					throw new RuntimeException();
				}

				ouTreeForm.setParent(change.getParentUuid());
				break;
			case DELETE:
				ouTreeForm = null;
				break;
		}
		
		return ouTreeForm;
	}

	private long countByAppliedStatus(AppliedStatus appliedStatus) {
		return futureChangesDao.countByAppliedStatus(appliedStatus);
	}

	private Comparator<OrgUnitFutureChange> getSorter() {
		return Comparator.comparing(OrgUnitFutureChange::getChangeDate).thenComparingLong(OrgUnitFutureChange::getId);
	}
}
