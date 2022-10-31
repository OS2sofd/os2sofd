package dk.digitalidentity.sofd.listener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import dk.digitalidentity.sofd.security.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.EntityChangeQueueDao;
import dk.digitalidentity.sofd.dao.model.ActiveDirectoryDetails;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.EntityChangeQueue;
import dk.digitalidentity.sofd.dao.model.EntityChangeQueueDetail;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.service.AffiliationService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class EntityListenerService {
	private static final String CHANGE_TYPE_UPDATE = "UPDATE";
	private static final String CHANGE_TYPE_CREATE = "CREATE";
	private static final String CHANGE_TYPE_DELETE = "DELETE"; // TODO: not implemented yet
	private static final String ENTITY_TYPE_ORGUNIT = "ORGUNIT";
	private static final String ENTITY_TYPE_PERSON = "PERSON";

	public enum ChangeType {
		CHANGED_NAME,                     // the name of the entity has changed
		ADDED_AFFILIATION,                // the person has a new employment
		CHANGED_AFFILIATION_LOCATION,     // the person has changed "location" (i.e. extra employment or moved orgUnit on existing employment)
		CHANGED_EMAIL,                    // the person has a changed or new email
		CHANGED_PARENT_UUID,			  // the orgunit has changed parent uuid
		CHANGED_AFFILIATION_STOP_DATE,    // the person has an existing affiliation where the stopdate has changed
		AD_PASSWORD_LOCKED,               // the person has an user AD account that has been password locked
		CHANGED_PNR,				      // the orgunit has changed pnr
		CHANGED_PHONES,					  // the person has changed or new phones
		CHANGED_MANAGER,                  // the orgunit has changed manager
		DELETED_TRUE                      // the orgunit has had deleted sat to true             
	}

	@Autowired
	private EntityChangeQueueDao entityChangeQueueDao;

	@Autowired
	private List<ListenerAdapter> adapters;
	
	@Autowired
	private SofdConfiguration configuration;

	public void emitCreateEvent(OrgUnit orgUnit) {
		EntityChangeQueue change = new EntityChangeQueue();
		change.setEntityUuid(orgUnit.getUuid());
		change.setChangeType(CHANGE_TYPE_CREATE);
		change.setEntityType(ENTITY_TYPE_ORGUNIT);
		change.setTts(new Date());

		entityChangeQueueDao.save(change);
	}

	public void emitUpdateEvent(OrgUnit oldOrgUnit, OrgUnit updatedOrgUnit) {
		EntityChangeQueue change = new EntityChangeQueue();
		change.setEntityUuid(oldOrgUnit.getUuid());
		change.setChangeType(CHANGE_TYPE_UPDATE);
		change.setEntityType(ENTITY_TYPE_ORGUNIT);
		change.setEntityChangeQueueDetails(new ArrayList<>());
		change.setTts(new Date());

		checkForOrgUnitNameChange(oldOrgUnit, updatedOrgUnit, change);
		checkForParentUuidChange(oldOrgUnit, updatedOrgUnit, change);
		checkForPnrChange(oldOrgUnit, updatedOrgUnit, change);
		checkForManagerChange(oldOrgUnit, updatedOrgUnit, change);
		checkForSetDeletedTrue(oldOrgUnit, updatedOrgUnit, change);
		
		if (change.getEntityChangeQueueDetails().size() > 0) {
			entityChangeQueueDao.save(change);
		}
	}

	public void emitCreateEvent(Person person) {
		EntityChangeQueue change = new EntityChangeQueue();
		change.setEntityUuid(person.getUuid());
		change.setChangeType(CHANGE_TYPE_CREATE);
		change.setEntityType(ENTITY_TYPE_PERSON);
		change.setTts(new Date());

		entityChangeQueueDao.save(change);
	}

	public void emitUpdateEvent(Person oldPerson, Person updatedPerson) {
		EntityChangeQueue change = new EntityChangeQueue();
		change.setEntityUuid(oldPerson.getUuid());
		change.setChangeType(CHANGE_TYPE_UPDATE);
		change.setEntityType(ENTITY_TYPE_PERSON);
		change.setEntityChangeQueueDetails(new ArrayList<>());
		change.setTts(new Date());
		
		checkForPersonNameChange(oldPerson, updatedPerson, change);
		
		checkForPersonEmailChange(oldPerson, updatedPerson, change);
		
		checkForPersonPhoneChange(oldPerson, updatedPerson, change);

		checkForPersonAddedAffiliation(oldPerson, updatedPerson, change);

		checkForPersonUserPasswordLocked(oldPerson, updatedPerson, change);

		checkForPersonAffiliationLocationChange(oldPerson, updatedPerson, change);

		checkForAffiliationStopDateChanges(oldPerson, updatedPerson, change);

		if (change.getEntityChangeQueueDetails().size() > 0) {
			entityChangeQueueDao.save(change);
		}
	}

	@Transactional
	public void emit() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, -1);
		Date oneMinuteAgo = cal.getTime();

		List<EntityChangeQueue> changes = entityChangeQueueDao.findByTtsBefore(oneMinuteAgo);
		if (changes == null || changes.size() == 0) {
			return;
		}

		Authentication authentication = SecurityUtil.getLoginSession();
		try {
			SecurityUtil.fakeLoginSession();
			for (EntityChangeQueue change : changes) {
				switch (change.getEntityType()) {
					case ENTITY_TYPE_ORGUNIT:
						switch (change.getChangeType()) {
							case CHANGE_TYPE_CREATE:
								for (ListenerAdapter adapter : adapters) {
									try {
										adapter.orgUnitCreated(change.getEntityUuid());
									}
									catch (Exception ex) {
										log.error("Event handling failed for OrgUnit " + change.getEntityUuid(), ex);
									}
								}
								break;
							case CHANGE_TYPE_DELETE:
								for (ListenerAdapter adapter : adapters) {
									try {
										adapter.orgUnitDeleted(change.getEntityUuid());
									}
									catch (Exception ex) {
										log.error("Event handling failed for OrgUnit " + change.getEntityUuid(), ex);
									}
								}
								break;
							case CHANGE_TYPE_UPDATE:
								for (ListenerAdapter adapter : adapters) {
									try {
										adapter.orgUnitUpdated(change.getEntityUuid(), change.getEntityChangeQueueDetails());
									}
									catch (Exception ex) {
										log.error("Event handling failed for OrgUnit " + change.getEntityUuid(), ex);
									}
								}
								break;
						}
						break;
					case ENTITY_TYPE_PERSON:
						switch (change.getChangeType()) {
							case CHANGE_TYPE_CREATE:
								for (ListenerAdapter adapter : adapters) {
									try {
										adapter.personCreated(change.getEntityUuid());
									}
									catch (Exception ex) {
										log.error("Event handling failed for Person " + change.getEntityUuid(), ex);
									}
								}
								break;
							case CHANGE_TYPE_DELETE:
								for (ListenerAdapter adapter : adapters) {
									try {
										adapter.personDeleted(change.getEntityUuid());
									}
									catch (Exception ex) {
										log.error("Event handling failed for Person " + change.getEntityUuid(), ex);
									}
								}
								break;
							case CHANGE_TYPE_UPDATE:
								for (ListenerAdapter adapter : adapters) {
									try {
										adapter.personUpdated(change.getEntityUuid(), change.getEntityChangeQueueDetails());
									}
									catch (Exception ex) {
										log.error("Event handling failed for Person " + change.getEntityUuid(), ex);
									}
								}
								break;
						}
						break;
				}

				entityChangeQueueDao.delete(change);;
			}
		}
		finally {
			SecurityUtil.setLoginSession(authentication);
		}
	}
	
	private void checkForAffiliationStopDateChanges(Person oldPerson, Person updatedPerson, EntityChangeQueue change) {
		List<Affiliation> opusAffiliations = oldPerson.getAffiliations().stream().filter(a -> configuration.getModules().getLos().getPrimeAffiliationMaster().equals(a.getMaster())).collect(Collectors.toList());
		
		for (Affiliation affiliation : opusAffiliations) {
			Optional<Affiliation> oAffiliation = updatedPerson.getAffiliations().stream()
					.filter(a -> Objects.equals(a.getMaster(), affiliation.getMaster()) && Objects.equals(a.getMasterId(), affiliation.getMasterId()))
					.findFirst();
			
			if (oAffiliation.isPresent()) {
				if (!Objects.equals(oAffiliation.get().getStopDate(), affiliation.getStopDate())) {
					EntityChangeQueueDetail entityChangeQueueDetail = new EntityChangeQueueDetail();
					entityChangeQueueDetail.setChangeType(ChangeType.CHANGED_AFFILIATION_STOP_DATE);
					entityChangeQueueDetail.setChangeTypeDetails(affiliation.getEmployeeId());
					entityChangeQueueDetail.setEntityChangeQueue(change);
					entityChangeQueueDetail.setNewValue(oAffiliation.get().getStopDate() == null ? null : oAffiliation.get().getStopDate().toString());
					entityChangeQueueDetail.setOldValue(affiliation.getStopDate() == null ? null : affiliation.getStopDate().toString());

					change.getEntityChangeQueueDetails().add(entityChangeQueueDetail);
				}
			}
		}
	}
	
	private void checkForPersonAddedAffiliation(Person oldPerson, Person updatedPerson, EntityChangeQueue change) {
		for (Affiliation affiliation : updatedPerson.getAffiliations()) {
			if (affiliation.isTransientFlagNewAffiliation()) {
				EntityChangeQueueDetail entityChangeQueueDetail = new EntityChangeQueueDetail();
				entityChangeQueueDetail.setChangeType(ChangeType.ADDED_AFFILIATION);
				entityChangeQueueDetail.setChangeTypeDetails(affiliation.getUuid());
				entityChangeQueueDetail.setEntityChangeQueue(change);

				change.getEntityChangeQueueDetails().add(entityChangeQueueDetail);
			}
		}
	}

	private void checkForPersonUserPasswordLocked(Person oldPerson, Person updatedPerson, EntityChangeQueue change) {
		for (User user : PersonService.getUsers(updatedPerson)) {
			// only relevant for AD accounts
			if (!SupportedUserTypeService.isActiveDirectory(user.getUserType())) {
				continue;
			}

			User oldUser = PersonService.getUsers(oldPerson).stream().filter(u -> Objects.equals(u.getUserType(), user.getUserType()) && Objects.equals(u.getMaster(), user.getMasterId())).findAny().orElse(null);

			if (oldUser != null) {
				ActiveDirectoryDetails oldActiveDirectoryDetails = oldUser.getActiveDirectoryDetails();
				ActiveDirectoryDetails updatedActiveDirectoryDetails = user.getActiveDirectoryDetails();
				
				if (oldActiveDirectoryDetails != null && oldActiveDirectoryDetails.isPasswordLocked() == false &&
					updatedActiveDirectoryDetails != null && updatedActiveDirectoryDetails.isPasswordLocked() == true) {
					EntityChangeQueueDetail entityChangeQueueDetail = new EntityChangeQueueDetail();
					entityChangeQueueDetail.setChangeType(ChangeType.AD_PASSWORD_LOCKED);
					entityChangeQueueDetail.setChangeTypeDetails(user.getMasterId());
					entityChangeQueueDetail.setEntityChangeQueue(change);

					change.getEntityChangeQueueDetails().add(entityChangeQueueDetail);
				}
			}
		}
	}

	private void checkForPersonAffiliationLocationChange(Person oldPerson, Person updatedPerson, EntityChangeQueue change) {
		// Check for at least one non disabled AD account
		boolean hasActiveAdAccount = PersonService.getUsers(oldPerson)
				.stream()
				.anyMatch(user -> !user.isDisabled() && SupportedUserTypeService.isActiveDirectory(user.getUserType()));

		if (!hasActiveAdAccount) {
			return;
		}

		if (AffiliationService.notStoppedAffiliations(oldPerson.getAffiliations()).size() > 0) {
			Map<String, Affiliation> oldAffiliationsMap = oldPerson.getAffiliations().stream().collect(Collectors.toMap(Affiliation::getUuid, affiliation -> affiliation));

			for (Affiliation updatedAffiliation : updatedPerson.getAffiliations()) {
				boolean changedAffiliation = false;

				if (!oldAffiliationsMap.containsKey(updatedAffiliation.getUuid())) {
					changedAffiliation = true;
				}
				else {
					// Check if the affiliation orgUnit has changed
					Affiliation oldAffiliation = oldAffiliationsMap.get(updatedAffiliation.getUuid());
					if (!Objects.equals(oldAffiliation.getOrgUnit().getUuid(), updatedAffiliation.getOrgUnit().getUuid())) {
						changedAffiliation = true;
					}
				}
				
				if (changedAffiliation) {
					// Check if the new affiliation is to an OrgUnit that the user does not already have affiliations
					Collection<Affiliation> oldAffiliations = oldAffiliationsMap.values();
					Set<String> affiliatedOuUuids = oldAffiliations
							.stream()
							.map(affiliation -> affiliation.getOrgUnit().getUuid()).collect(Collectors.toSet());

					// If the new affiliations OrgUnit is not already affiliated create QueueDetail
					if (!affiliatedOuUuids.contains(updatedAffiliation.getOrgUnit().getUuid())) {
						EntityChangeQueueDetail entityChangeQueueDetail = new EntityChangeQueueDetail();
						entityChangeQueueDetail.setChangeType(ChangeType.CHANGED_AFFILIATION_LOCATION);
						entityChangeQueueDetail.setChangeTypeDetails(updatedAffiliation.getUuid());
						entityChangeQueueDetail.setEntityChangeQueue(change);

						change.getEntityChangeQueueDetails().add(entityChangeQueueDetail);
					}
				}
			}
		}
	}

	private void checkForPersonNameChange(Person oldPerson, Person updatedPerson, EntityChangeQueue change) {
		if (!Objects.equals(PersonService.getName(oldPerson), PersonService.getName(updatedPerson))) {
			EntityChangeQueueDetail entityChangeQueueDetail = new EntityChangeQueueDetail();
			entityChangeQueueDetail.setChangeType(ChangeType.CHANGED_NAME);
			entityChangeQueueDetail.setEntityChangeQueue(change);
			
			change.getEntityChangeQueueDetails().add(entityChangeQueueDetail);
		}
	}
	
	private void checkForPersonEmailChange(Person oldPerson, Person updatedPerson, EntityChangeQueue change) {
		String oldEmail = PersonService.getEmail(oldPerson);
		String newEmail = PersonService.getEmail(updatedPerson);

		if (!Objects.equals(oldEmail, newEmail)) {
			EntityChangeQueueDetail entityChangeQueueDetail = new EntityChangeQueueDetail();
			entityChangeQueueDetail.setChangeType(ChangeType.CHANGED_EMAIL);
			entityChangeQueueDetail.setOldValue(oldEmail);
			entityChangeQueueDetail.setNewValue(newEmail);
			entityChangeQueueDetail.setEntityChangeQueue(change);

			change.getEntityChangeQueueDetails().add(entityChangeQueueDetail);
		}
	}
	
	private void checkForPersonPhoneChange(Person oldPerson, Person updatedPerson, EntityChangeQueue change) {
		List<String> oldPhones = oldPerson.getPhones().stream().map(p -> p.getPhone()).map(p -> p.getPhoneNumber()).collect(Collectors.toList());
		List<String> newPhones = updatedPerson.getPhones().stream().map(p -> p.getPhone()).map(p -> p.getPhoneNumber()).collect(Collectors.toList());
		boolean changes = false;
		
		for (String oldPhone : oldPhones) {
			if (!newPhones.contains(oldPhone)) {
				changes = true;
				break;
			}
		}
		
		if (!changes) {
			for (String newPhone : newPhones) {
				if (!oldPhones.contains(newPhone)) {
					changes = true;
					break;
				}
			}
		}
		
		if (changes) {
			EntityChangeQueueDetail entityChangeQueueDetail = new EntityChangeQueueDetail();
			entityChangeQueueDetail.setChangeType(ChangeType.CHANGED_PHONES);
			entityChangeQueueDetail.setEntityChangeQueue(change);

			change.getEntityChangeQueueDetails().add(entityChangeQueueDetail);
		}
	}
	
	private void checkForOrgUnitNameChange(OrgUnit oldOrgUnit, OrgUnit updatedOrgUnit, EntityChangeQueue change) {
		if (!Objects.equals(oldOrgUnit.getName(), updatedOrgUnit.getName())) {
			EntityChangeQueueDetail entityChangeQueueDetail = new EntityChangeQueueDetail();
			entityChangeQueueDetail.setChangeType(ChangeType.CHANGED_NAME);
			entityChangeQueueDetail.setOldValue(oldOrgUnit.getName());
			entityChangeQueueDetail.setNewValue(updatedOrgUnit.getName());
			entityChangeQueueDetail.setEntityChangeQueue(change);

			change.getEntityChangeQueueDetails().add(entityChangeQueueDetail);
		}
	}
	
	private void checkForParentUuidChange(OrgUnit oldOrgUnit, OrgUnit updatedOrgUnit, EntityChangeQueue change) {
		if (updatedOrgUnit.getParent() != null) {
			if (oldOrgUnit.getParent() != null) {
				if (!Objects.equals(oldOrgUnit.getParent().getUuid(), updatedOrgUnit.getParent().getUuid())) {
					createAndAdd(oldOrgUnit.getParent().getName(), updatedOrgUnit.getParent().getName(), change);
				}
			}
			else {
				createAndAdd(null, updatedOrgUnit.getParent().getName(), change);
			}
			
		}else {
			if (oldOrgUnit.getParent() != null) {
				createAndAdd(oldOrgUnit.getParent().getName(), null, change);
			}
		}
	}
	
	private void createAndAdd(String oldValue, String newValue, EntityChangeQueue change) {
		EntityChangeQueueDetail entityChangeQueueDetail = new EntityChangeQueueDetail();
		entityChangeQueueDetail.setChangeType(ChangeType.CHANGED_PARENT_UUID);
		entityChangeQueueDetail.setOldValue(oldValue);
		entityChangeQueueDetail.setNewValue(newValue);
		entityChangeQueueDetail.setEntityChangeQueue(change);

		change.getEntityChangeQueueDetails().add(entityChangeQueueDetail);
	}
	
	private void checkForPnrChange(OrgUnit oldOrgUnit, OrgUnit updatedOrgUnit, EntityChangeQueue change) {
		if (!Objects.equals(oldOrgUnit.getPnr(), updatedOrgUnit.getPnr())) {
			EntityChangeQueueDetail entityChangeQueueDetail = new EntityChangeQueueDetail();
			entityChangeQueueDetail.setChangeType(ChangeType.CHANGED_PNR);
			entityChangeQueueDetail.setOldValue(oldOrgUnit.getPnr() != null ? oldOrgUnit.getPnr().toString() : "" );
			entityChangeQueueDetail.setNewValue(updatedOrgUnit.getPnr() != null ? updatedOrgUnit.getPnr().toString() : "");
			entityChangeQueueDetail.setEntityChangeQueue(change);

			change.getEntityChangeQueueDetails().add(entityChangeQueueDetail);
		}
	}
	
	private void checkForManagerChange(OrgUnit oldOrgUnit, OrgUnit updatedOrgUnit, EntityChangeQueue change) {
		Person oldManager = oldOrgUnit.getManager() == null ? null : oldOrgUnit.getManager().getManager();
		Person newManager = updatedOrgUnit.getManager() == null ? null : updatedOrgUnit.getManager().getManager();
		
		if ((oldManager == null && newManager != null) || (oldManager != null && newManager != null && !oldManager.getUuid().equals(newManager.getUuid()))) {
			EntityChangeQueueDetail entityChangeQueueDetail = new EntityChangeQueueDetail();
			entityChangeQueueDetail.setChangeType(ChangeType.CHANGED_MANAGER);
			entityChangeQueueDetail.setOldValue(oldManager != null ? oldManager.getUuid() : "");
			entityChangeQueueDetail.setNewValue(newManager != null ? newManager.getUuid() : "");
			entityChangeQueueDetail.setEntityChangeQueue(change);

			change.getEntityChangeQueueDetails().add(entityChangeQueueDetail);
		}
	}
	
	private void checkForSetDeletedTrue(OrgUnit oldOrgUnit, OrgUnit updatedOrgUnit, EntityChangeQueue change) {
		if (!oldOrgUnit.isDeleted() && updatedOrgUnit.isDeleted()) {
			EntityChangeQueueDetail entityChangeQueueDetail = new EntityChangeQueueDetail();
			entityChangeQueueDetail.setChangeType(ChangeType.DELETED_TRUE);
			entityChangeQueueDetail.setOldValue("false");
			entityChangeQueueDetail.setNewValue("true");
			entityChangeQueueDetail.setEntityChangeQueue(change);

			change.getEntityChangeQueueDetails().add(entityChangeQueueDetail);
		}
	}
}
