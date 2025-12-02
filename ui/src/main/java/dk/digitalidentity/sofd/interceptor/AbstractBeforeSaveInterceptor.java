package dk.digitalidentity.sofd.interceptor;

import static dk.digitalidentity.sofd.util.NullChecker.getValue;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import dk.digitalidentity.sofd.dao.model.enums.AccountOrderDeactivateAndDeleteRule;
import dk.digitalidentity.sofd.service.ProfessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.AffiliationDao;
import dk.digitalidentity.sofd.dao.OrgUnitDao;
import dk.digitalidentity.sofd.dao.OrgUnitTypeDao;
import dk.digitalidentity.sofd.dao.PersonDao;
import dk.digitalidentity.sofd.dao.model.AccountOrder;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.ModificationHistory;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.OrgUnitType;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.exception.InsufficientAccessRightException;
import dk.digitalidentity.sofd.listener.EntityListenerService;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.AccountOrderService;
import dk.digitalidentity.sofd.service.ModificationHistoryService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.OrganisationService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.PrimeService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import dk.digitalidentity.sofd.service.model.ChangeType;

@Component
public class AbstractBeforeSaveInterceptor {

	@Autowired
	protected PrimeService primeService;

	@Autowired
	private SupportedUserTypeService userTypeService;

	@Autowired
	private AccountOrderService accountOrderService;

	@Autowired
	private OrgUnitService orgUnitService;

	@Autowired
	private OrgUnitDao orgUnitDao;

	@Autowired
	private OrganisationService organisationService;

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private OrgUnitTypeDao orgUnitTypeDao;

	@Autowired
	private PersonDao personDao;

	@Autowired
	private EntityListenerService entityListenerService;

	@Autowired
	private AbstractBeforeSaveInterceptor self;

	@Autowired
	private AffiliationDao affiliationDao;
	
	@Autowired
	private ModificationHistoryService modificationHistoryService;

	@Autowired
	ProfessionService professionService;

	@Transactional
	public void handleSavePerson(Person person) {
		updateTransientFlags(person);

		// make sure we have a copy of the old person - any of the code below might trigger a flush as a side-effect
		Person oldPerson = self.loadOldPerson(person.getUuid());

		if (person.getUuid() == null) {
			// attempt to use primary AD user as UUID
			User user = PersonService.getUsers(person).stream().filter(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType())).findFirst().orElse(null);
			if (user != null) {
				try {
					String potentialUuid = user.getMasterId();
					
					// validate UUID format - this throws an exception
					UUID.fromString(potentialUuid);
					
					// verify that no other person has the same uuid (could happen if they changed the CPR on an AD account)
					Person alreadyExists = personDao.findByUuid(potentialUuid);
					if (alreadyExists == null) {
						person.setUuid(potentialUuid);
					}
				}
				catch (Exception ignored) {
					;
				}
			}

			// fallback to random UUID
			if (person.getUuid() == null) {
				person.setUuid(UUID.randomUUID().toString());
			}
		}

		if (person.getAffiliations() != null) {
			for (Affiliation affiliation : person.getAffiliations()) {
				// preload orgunit affiliations for later use
				if (affiliation.getOrgUnit() != null &&
						affiliation.getOrgUnit().getAffiliations() != null) {
					affiliation.getOrgUnit().getAffiliations().size();
				}
				if (affiliation.getCalculatedOrgUnit() != null &&
						affiliation.getCalculatedOrgUnit().getAffiliations() != null) {
					affiliation.getCalculatedOrgUnit().getAffiliations().size();
				}

				// set default uuid
				if (affiliation.getUuid() == null || affiliation.getUuid().isEmpty()) {
					affiliation.setUuid(UUID.randomUUID().toString());
				}

				// set default employeeId to uuid
				if (!StringUtils.hasLength(affiliation.getEmployeeId())) {
					affiliation.setEmployeeId(affiliation.getUuid());
				}

				if (affiliation.getPerson() == null) {
					affiliation.setPerson(person);
				}

				// set default deactivateAndDeleteRule according to config
				if (affiliation.getDeactivateAndDeleteRule() == null) {
					// always set OS2vikar to keep alive - otherwise follow configuration
					var deactivateAndDeleteRule = affiliation.getMaster().equalsIgnoreCase(configuration.getModules().getSubstitute().getMasterId())
							? AccountOrderDeactivateAndDeleteRule.KEEP_ALIVE
							: configuration.getScheduled().getAccountOrderGeneration().getAffiliationDeactivateAndDeleteRuleDefault();
					affiliation.setDeactivateAndDeleteRule(deactivateAndDeleteRule);
				}
			}
		}

		if (person.getUsers() != null) {
			for (String userType : userTypeService.getAllUserTypes()) {
				List<User> users = PersonService.getUsers(person).stream().filter(u -> u.getUserType().equals(userType)).collect(Collectors.toList());
				
				if (users != null && users.size() > 0) {
					primeService.setPrimeUser(users);
				}
			}
		}

		// remove invalid affiliations
		// NOTE: for some reason this code has to be after userTypeService.getAllUserTypes() otherwise there will be an error
		// object references an unsaved transient instance - save the transient instance before flushing: dk.digitalidentity.sofd.dao.model.Affiliation
		if (person.getAffiliations() != null) {
			for (Iterator<Affiliation> iterator = person.getAffiliations().iterator(); iterator.hasNext();) {
				Affiliation affiliation = iterator.next();
				
				if (!"OPUS".equals(affiliation.getMaster())) {
					continue;
				}
				
				// remove invalid affiliations
				if (affiliation.getStartDate()!= null && affiliation.getStopDate() != null) {
					LocalDate stopDate = toLocalDate(affiliation.getStopDate());
					LocalDate startDate = toLocalDate(affiliation.getStartDate());
					
					if (startDate != null && stopDate != null && startDate.isEqual(stopDate)) {
						iterator.remove();
						affiliationDao.delete(affiliation);
					}
				}
			}
		}

		// empty chosenNames are NULL'ed to ensure consistency with PersonService.getName(person)
		if ("".equals(person.getChosenName())) {
			person.setChosenName(null);
		}

		// first name and last name cannot be empty
		if (person.getFirstname() == null || person.getFirstname().length() == 0) {
			person.setFirstname("Ukendt");
		}

		if (person.getSurname() == null || person.getSurname().length() == 0) {
			person.setSurname("Ukendt");
		}

		primeService.setPrimePhone(person);
		primeService.setPrimePost(person);
		
		// have to be after, so we can sort by UUID when picking a random affiliation :)
		primeService.setPrimeAffilation(person);

		// do this last! reading the professions from database apparently flushes the person to database before our interceptor is done
		if (person.getAffiliations() != null) {
			for (Affiliation affiliation : person.getAffiliations()) {
				// update affiliation profession
				professionService.updateAffiliation(affiliation);
			}
		}

		// emit update notifications
		if (oldPerson != null) {
			entityListenerService.emitUpdateEvent(oldPerson, person);
		}
		else {
			entityListenerService.emitCreateEvent(person);
		}
		
		ModificationHistory modificationHistory = new ModificationHistory();
		modificationHistory.setEntity(EntityType.PERSON);
		modificationHistory.setUuid(person.getUuid());
		modificationHistory.setChanged(new Date());
		modificationHistory.setChangeType(oldPerson != null ? ChangeType.UPDATE : ChangeType.CREATE);

		modificationHistoryService.insert(modificationHistory);
	}

	public void handleAccountOrders(Person person) {
		// we only do this when saving persons, not OrgUnits, because the order-account-settings are not part
		// of the actual OrgUnit data-structure, and editing those will not trigger this event. Instead we
		// also call this method from the actual service that sets these values on the OrgUnit
		if (person.getAffiliations() != null && person.isDisableAccountOrdersCreate() == false) {
			List<AccountOrder> orderAccounts = accountOrderService.getAccountsToCreate(person.getAffiliations(), true, false);

			if (orderAccounts != null && orderAccounts.size() > 0) {

				// if there are any completed OPUS orders on the table (14 days retention), we remove it from
				// the orders to avoid creating new accounts over and over and over until we get the update from the OPUS file
				List<AccountOrder> completedOpusCreateOrders = accountOrderService.findAllCompletedOpusCreateOrders(person);

				if (completedOpusCreateOrders != null && completedOpusCreateOrders.size() > 0) {
					List<String> employeeIdsWithOrders = completedOpusCreateOrders.stream().map(a -> a.getEmployeeId()).collect(Collectors.toList());

					for (Iterator<AccountOrder> iterator = orderAccounts.iterator(); iterator.hasNext(); ) {
						AccountOrder accountOrder = iterator.next();

						if (!SupportedUserTypeService.isOpus(accountOrder.getUserType())) {
							continue;
						}

						if (employeeIdsWithOrders.contains(accountOrder.getEmployeeId())) {
							iterator.remove();
						}
					}
				}

				accountOrderService.save(orderAccounts);
			}
		}
		// we need to trigger the account order cleanup whenever an affiliation is updated.
		// since the affiliation can be updated directly by Affiliation dao, but also by Person dao, we place the cleanup listener here
		// to make sure cleanup is invoked whenever a person is updated.
		accountOrderService.cleanup();
	}

	@Transactional
	public void handleSaveOrgUnit(OrgUnit orgUnit) {

		if (!SecurityUtil.canEdit(orgUnit)) {
			throw new InsufficientAccessRightException(SecurityUtil.getUsername() + " : Not allowed to edit OrgUnit " + orgUnit.getUuid());
		}

		// make sure we have a copy of the old OrgUnit - any of the code below might trigger a flush operation
		OrgUnit oldOrgUnit = self.loadOldOrgUnit(orgUnit.getUuid());

		// always update last changed
		orgUnit.setLastChanged(new Date());

		if (orgUnit.getUuid() == null) {
			orgUnit.setUuid(UUID.randomUUID().toString());
		}

		// if the municipality has enabled LOS match, we allow them to match LOS IDs
		// to types inside SOFD Core
		if (configuration.getIntegrations().getOpus().isEnableLosIdMatch()) {
			if (orgUnit.getOrgTypeId() != null) {
				String extId = Long.toString(orgUnit.getOrgTypeId());

				OrgUnitType orgUnitType = orgUnitTypeDao.getByExtId(extId);
				if (orgUnitType != null) {
					orgUnit.setType(orgUnitType);
				}
			}
		}

		// default to AFDELING
		if (orgUnit.getType() == null) {
			orgUnit.setType(orgUnitService.getDepartmentType());
		}

		if (orgUnit.getBelongsTo() == null) {
			orgUnit.setBelongsTo(organisationService.getAdmOrg());
		}

		primeService.setPrimePhone(orgUnit);
		primeService.setPrimePost(orgUnit);
		primeService.setPrimeEan(orgUnit);

		// emit update notifications
		if (oldOrgUnit != null) {
			entityListenerService.emitUpdateEvent(oldOrgUnit, orgUnit);
		}
		else {
			entityListenerService.emitCreateEvent(orgUnit);
		}
		
		ModificationHistory modificationHistory = new ModificationHistory();
		modificationHistory.setEntity(EntityType.ORGUNIT);
		modificationHistory.setUuid(orgUnit.getUuid());
		modificationHistory.setChanged(new Date());
		modificationHistory.setChangeType(oldOrgUnit != null ? ChangeType.UPDATE : ChangeType.CREATE);

		modificationHistoryService.insert(modificationHistory);
	}

	// we ensure a fresh copy is loaded by setting the propagation, but we also need
	// to ensure that any data that EntityListenerService needs is loaded, so do some
	// force-loading as needed. No reason to load fields that EntityListenerService
	// does not need though
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public OrgUnit loadOldOrgUnit(String uuid) {
		OrgUnit orgUnit = orgUnitDao.findByUuid(uuid);
		if (orgUnit != null) {
			getValue(() -> orgUnit.getParent().getName());
			getValue(() -> orgUnit.getManager().getManager().getCpr());
		}

		return orgUnit;
	}

	// we ensure a fresh copy is loaded by setting the propagation, but we also need
	// to ensure that any data that EntityListenerService needs is loaded, so do some
	// force-loading as needed. No reason to load fields that EntityListenerService
	// does not need though
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Person loadOldPerson(String uuid) {
		Person person = personDao.findByUuid(uuid);
		if (person != null) {
			for (Affiliation affiliation : person.getAffiliations()) {
				affiliation.getOrgUnit().getUuid();
				affiliation.getCalculatedOrgUnit().getUuid();
			}

			person.getChildren().size();
			PersonService.getPhones(person).stream().forEach(p -> p.getPhoneNumber());
			PersonService.getUsers(person).stream().forEach(u -> {
				if (u.getActiveDirectoryDetails() != null) {
					u.getActiveDirectoryDetails().getId();
				} else {
					u.getUserId();
				}
			});
		}

		return person;
	}
	
	private void updateTransientFlags(Person person) {
		if (person.getAffiliations() != null && person.getAffiliations().size() > 0) {
			for (Affiliation affiliation : person.getAffiliations()) {
				affiliation.setTransientFlagNewAffiliation(affiliation.getId() == 0);
			}
		}
	}

	private LocalDate toLocalDate(Date date) {
		if (date == null) {
			return null;
		}

		// might be an SQL instance, so convert to something that has a toInstant()
		// method on it
		if (date instanceof java.sql.Date) {
			date = new Date(date.getTime());
		}

		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}
}
