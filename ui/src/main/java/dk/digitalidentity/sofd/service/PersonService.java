package dk.digitalidentity.sofd.service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.history.Revision;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import dk.digitalidentity.sofd.config.RoleConstants;
import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.ActiveDirectoryDetailsDao;
import dk.digitalidentity.sofd.dao.OrgUnitManagerDao;
import dk.digitalidentity.sofd.dao.PersonDao;
import dk.digitalidentity.sofd.dao.ReservedUsernameDao;
import dk.digitalidentity.sofd.dao.model.AccountOrder;
import dk.digitalidentity.sofd.dao.model.ActiveDirectoryDetails;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.EmailTemplate;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import dk.digitalidentity.sofd.dao.model.ModificationHistory;
import dk.digitalidentity.sofd.dao.model.Notification;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.OrgUnitManager;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.Phone;
import dk.digitalidentity.sofd.dao.model.Post;
import dk.digitalidentity.sofd.dao.model.RevisionId;
import dk.digitalidentity.sofd.dao.model.SubstituteAssignment;
import dk.digitalidentity.sofd.dao.model.SubstituteAssignmentOrgUnitMapping;
import dk.digitalidentity.sofd.dao.model.SupportedUserType;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.UserChangeEmployeeIdQueue;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderStatus;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderType;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplatePlaceholder;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplateType;
import dk.digitalidentity.sofd.dao.model.enums.EndDate;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.NotificationType;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.model.ChangeType;
import dk.digitalidentity.sofd.service.model.PersonDeletePeriod;
import dk.digitalidentity.sofd.telephony.controller.rest.dto.AutoCompleteResult;
import dk.digitalidentity.sofd.telephony.controller.rest.dto.ValueData;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@EnableCaching
@EnableScheduling
@Service
public class PersonService {

	@Autowired
	private PersonDao personDao;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private UserService userService;

	@Autowired
	private PersonService self;

	@Autowired
	private SupportedUserTypeService supportedUserTypeService;

	@Autowired
	private AccountOrderService accountOrderService;

	@Autowired
	private SofdConfiguration sofdConfiguration;

	@Autowired
	private EmailTemplateService emailTemplateService;

	@Autowired
	private EmailService emailService;

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private UserChangeEmployeeIdQueueService userChangeEmployeeIdQueueService;

	@Autowired
	private PhotoService photoService;

	@Autowired
	private OrgUnitService orgUnitService;

	@Autowired
	private AffiliationService affiliationService;

	@Autowired
	private ActiveDirectoryDetailsDao activeDirectoryDetailsDao;

	@Autowired
	private SettingService settingService;

	@Autowired
	private OrgUnitManagerDao orgUnitManagerDao;

	@Autowired
	private ModificationHistoryService modificationHistoryService;

	@Autowired
	private ReservedUsernameDao reservedUsernameDao;

	public List<Person> findByUserType(String userType) {
		return personDao.findDistinctByUsersUserUserTypeAndDeletedFalse(userType);
	}

	public List<Person> findByUserTypeAndUserId(String userType, String userId) {
		return personDao.findByUsersUserUserTypeAndUsersUserUserId(userType, userId);
	}

	public Person findByUser(User user) {
		return personDao.findByUsersUser(user);
	}

	public Person findByKombitUuid(String kombitUuid) {
		return personDao.findByUsersUserActiveDirectoryDetailsKombitUuid(kombitUuid);
	}

	public Person getByUuid(String uuid) {
		return personDao.findByUuid(uuid);
	}

	public Person findByCpr(String cpr) {
		return personDao.findByCpr(cpr);
	}

	public Page<Person> getAll(Pageable pageable) {
		return personDao.findAll(pageable);
	}

	public List<Person> getAll() {
		return personDao.findAll();
	}

	public List<Person> getActive() {
		return personDao.findByDeletedFalse();
	}

	public List<Person> getByPhoneMasterAndMasterId(String master, String masterId) {
		return personDao.findByPhonesPhoneMasterAndPhonesPhoneMasterId(master, masterId);
	}

	// TODO: can remove this once RKSK is migrated
	@Transactional
	public void migrateUuids() {
		List<ActiveDirectoryDetails> toSave = new ArrayList<>();

		for (Person person : getAll()) {
			List<User> users = PersonService.getUsers(person)
					.stream()
					.filter(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType()))
					.collect(Collectors.toList());

			if (users.size() == 0) {
				continue;
			}

			if (users.size() == 1) {
				// if there is only a single user, we always copy, even if that is not prime (because it is disabled)
				User user = users.get(0);

				user.getActiveDirectoryDetails().setKombitUuid(person.getUuid());
				toSave.add(user.getActiveDirectoryDetails());
			}
			else {
				Set<String> seenUuids = new HashSet<>();

				// Handle prime user first
				Collections.sort(users, new Comparator<User>() {
					public int compare(User u1, User u2) {
						return Boolean.compare(u2.isPrime(), u1.isPrime());
					};
				});

				for (User user : users) {
					if (!StringUtils.hasLength(user.getActiveDirectoryDetails().getKombitUuid())) {
						String uuidCandidate = null;

						if (user.isPrime()) {
							// SOFDs current UUID
							uuidCandidate = person.getUuid();
						}
						else {
							// ObjectGuid from AD
							uuidCandidate = user.getMasterId();
						}

						// if the UUID has already been used, pick a random one (there is a special corner case where the persons UUID matches one
						// of the non-prime AD accounts UUID, which results in duplicates)
						if (seenUuids.contains(uuidCandidate)) {
							uuidCandidate = UUID.randomUUID().toString();
						}

						seenUuids.add(uuidCandidate);
						user.getActiveDirectoryDetails().setKombitUuid(uuidCandidate);

						toSave.add(user.getActiveDirectoryDetails());
					}
				}
			}
		}

		activeDirectoryDetailsDao.saveAll(toSave);
	}

	@Cacheable(value = "activePersons")
	public List<Person> getActiveCached() {
		List<Person> persons = personDao.findByDeletedFalse();

		// force-load all users and affiliations
		for (Person person : persons) {
			for (User user : PersonService.getUsers(person)) {
				user.getUserId();
			}

			if (person.getLeave() != null) {
				person.getLeave().getReason();
			}

			for (Affiliation affiliation : person.getAffiliations()) {
				affiliation.getOrgUnit().getName();
				affiliation.getWorkplaces().size();

				if (affiliation.getAlternativeOrgUnit() != null) {
					affiliation.getAlternativeOrgUnit().getName();
				}
			}
		}

		return persons;
	}

    @Scheduled(fixedRate = 60 * 60 * 1000)
    public void resetActivePersonCacheTask() {
    	self.resetActivePersonCache();
    }

    @CacheEvict(value = "activePersons", allEntries = true)
    public void resetActivePersonCache() {
    	; // clears cache every hour - we want to protect
          // against force-refresh in the browser, as the lookup
          // can be a bit intensive
    }

    public List<Person> findAllWithADPwdChangePending(String date) {
    	return personDao.getPersonsWithADPwdChangePending(date);
    }

	public List<Person> findAllManagers() {
		return personDao.getManagers();
	}

	public List<OrgUnitManager> findAllManagersWithOrgUnits() {
		return orgUnitManagerDao.findByInheritedFalse();
	}

	// this method should ONLY be used in the SynchronizeOrgUnitManagersTask that synchronizes affiliation managers to orgunit managers
	// other methods should use the findAllManagers method instead
	public List<Person> findAllAffiliationManagers() {
		return personDao.getAffiliationManagers();
	}

	public List<Person> findAllTRs() {
		return personDao.getTRs();
	}

	public List<Person> findAllSRs() {
		return personDao.getSRs();
	}

	public boolean isSubstituteInSofd(Person person) {
		return (personDao.countSofdSubstituteAssignments(person.getUuid()) > 0);
	}

	public Person save(Person person) {
		return personDao.save(person);
	}

	public static List<User> getUsers(Person person) {
		if (person.getUsers() == null) {
			return new ArrayList<>();
		}

		return person.getUsers().stream().map(u -> u.getUser()).collect(Collectors.toList());
	}

	public static String getEmail(Person person) {
		Optional<User> user = PersonService.getUsers(person).stream().filter(u -> SupportedUserTypeService.isExchange(u.getUserType()) && u.isPrime()).findFirst();
		if (user.isPresent()) {
			return user.get().getUserId();
		}
		else {
			user = PersonService.getUsers(person).stream().filter(u -> SupportedUserTypeService.isSchoolEmail(u.getUserType()) && u.isPrime()).findFirst();
			if (user.isPresent()) {
				return user.get().getUserId();
			}
		}

		return null;
	}

	public static List<Phone> getPhones(Person person) {
		if (person.getPhones() == null) {
			return new ArrayList<>();
		}

		return person.getPhones().stream().map(p -> p.getPhone()).collect(Collectors.toList());
	}

	public static String getName(Person person) {
		if (person.getChosenName() != null && person.getChosenName().length() > 0) {
			return person.getChosenName();
		}

		return getCprName(person);
	}

	public static String getCprName(Person person) {
		return person.getFirstname() + " " + person.getSurname();
	}

	@Transactional
	public void cleanupDeletedPersons() {
		int months = 0;
		PersonDeletePeriod interval = settingService.getPersonDeletePeriod();
		switch (interval) {
			case MONTH_6:
				months = 6;
				break;
			case MONTH_12:
				months = 12;
				break;
			case MONTH_36:
				months = 36;
				break;
			case MONTH_60:
				months = 60;
				break;
			case NEVER:
				return;
			default:
				return;
		}

		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.MONTH, -1 * months);
		Date beforeDate = cal.getTime();

		List<Person> deletedPersons = findAllDeleted();

		ArrayList<Person> toBeDeleted = new ArrayList<>();
		for (Person person : deletedPersons) {
			if (person.getLastChanged() == null || person.getLastChanged().before(beforeDate)) {
				log.info("Physical delete of " + PersonService.getName(person) + ", uuid=" + person.getUuid());

				toBeDeleted.add(person);
			}
		}

		hardDeletePersons(toBeDeleted);
		deletePersonsLogs(toBeDeleted);
	}

	@Transactional
	public void updatePersonFromCpr(Person person, Post post, String firstname, String surname, boolean dead, boolean disenfranchised) {
        log.info("Updating cpr data on person " + person.getUuid());

		if (!SecurityUtil.isUserLoggedIn() && !SecurityUtil.isClientLoggedIn()) {
			SecurityUtil.fakeLoginSession();
		}

		if( post != null ) {
			if (person.getRegisteredPostAddress() != null) {
				person.getRegisteredPostAddress().setAddressProtected(post.isAddressProtected());
				person.getRegisteredPostAddress().setCity(post.getCity());
				person.getRegisteredPostAddress().setCountry(post.getCountry());
				person.getRegisteredPostAddress().setLocalname(post.getLocalname());
				person.getRegisteredPostAddress().setMaster(post.getMaster());
				person.getRegisteredPostAddress().setMasterId(post.getMasterId());
				person.getRegisteredPostAddress().setPostalCode(post.getPostalCode());
				person.getRegisteredPostAddress().setStreet(post.getStreet());
			}
			else {
				person.setRegisteredPostAddress(post);
			}
		}

		if (!Objects.equals(person.isDead(), dead)) {
			person.setDead(dead);

			if (dead) {
				sendDeadOrDisenfranchisedNotification(person, NotificationType.PERSON_DEAD, "Personen er blevet meldt død eller bortkommet i cpr registeret");

				if( person.getUsers().stream().anyMatch(u -> SupportedUserTypeService.isActiveDirectory(u.getUser().getUserType())) ) {
					sendDeadOrDisenfranchisedNotification(person, NotificationType.PERSON_DEAD_AD_ONLY, "Personen er registreret med en AD-konto og er blevet meldt død eller bortkommet i cpr registeret");
				}
			}
		}

		if (!Objects.equals(person.isDisenfranchised(), disenfranchised)) {
			person.setDisenfranchised(disenfranchised);

			if (disenfranchised) {
				sendDeadOrDisenfranchisedNotification(person, NotificationType.PERSON_DISENFRANCHISED, "Personen er blevet meldt umyndiggjort i cpr registeret");

				if( person.getUsers().stream().anyMatch(u -> SupportedUserTypeService.isActiveDirectory(u.getUser().getUserType())) ) {
					sendDeadOrDisenfranchisedNotification(person, NotificationType.PERSON_DISENFRANCHISED_AD_ONLY, "Personen er registreret med en AD-konto og er blevet meldt umyndiggjort i cpr registeret");
				}
			};
		}

		if (dead || disenfranchised) {

			// set expiry on all not disabled AD accounts
			List<AccountOrder> newOrders = new ArrayList<AccountOrder>();
			List<AccountOrder> orders = generateExpireOrders(person, new Date(), new Date());
			newOrders.addAll(orders);

			person.setDisableAccountOrders(true);

			// we have to loop (there won't be many) as the saveAll method has some nasty side-effects
			for (AccountOrder order : newOrders) {
				accountOrderService.save(order);
			}
		}

		if( !Objects.equals(person.getFirstname(), firstname) || !Objects.equals(person.getSurname(), surname) ) {
			person.setFirstname(firstname);
			person.setSurname(surname);

			// name was changed in CPR - reset chosen name according to config
			if (configuration.getModules().getPerson().isResetChosenNameOnNameChange()) {
				person.setChosenName(null);
			}

			// if person has no users - delete reserved usernames so they will get generated from new name
			if( person.getUsers().size() == 0 ) {
				reservedUsernameDao.deleteByPersonUuid(person.getUuid());
			}
		}
		person.setUpdatedFromCpr(true);
		self.save(person);
	}

	public void sendDeadOrDisenfranchisedNotification(Person person, NotificationType type, String message) {
		Notification notification = new Notification();
		notification.setActive(true);
		notification.setAffectedEntityName(getName(person));
		notification.setAffectedEntityType(EntityType.PERSON);
		notification.setAffectedEntityUuid(person.getUuid());
		notification.setMessage(message);
		notification.setCreated(new Date());
		notification.setNotificationType(type);
		notificationService.save(notification);
	}

	public List<RevisionId> getRevisionIds(String uuid) {
		return personDao.getRevisionIds(uuid);
	}

	public Revision<Integer, Person> findRevision(String uuid, Integer revId) {
		return personDao.findRevision(uuid, revId).orElse(null);
	}

	public List<Person> findTop10ByName(String term) {
		return personDao.findTop10ByName(term);
	}

	public List<Person> findAllDeleted() {
		return personDao.findByDeletedTrue();
	}

	public List<Person> findAllTaxed() {
		return personDao.findByTaxedPhoneTrue();
	}

	@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
	public void hardDeletePersons(List<Person> entities) {
		Set<String> personUuids = entities.stream().map(p -> p.getUuid()).collect(Collectors.toSet());

		for (String uuid : personUuids) {
			ModificationHistory modificationHistory = new ModificationHistory();
			modificationHistory.setEntity(EntityType.PERSON);
			modificationHistory.setUuid(uuid);
			modificationHistory.setChanged(new Date());
			modificationHistory.setChangeType(ChangeType.DELETE);

			modificationHistoryService.insert(modificationHistory);
		}

		personDao.deleteByUuid(personUuids);
	}

	@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
	public void deletePersonsLogs(List<Person> entities) {
		for (Person person : entities) {
			personDao.deletePersonLog(person.getUuid());

			Notification notification = new Notification();
			notification.setActive(true);
			notification.setAffectedEntityName(getName(person));
			notification.setAffectedEntityType(EntityType.PERSON);
			notification.setAffectedEntityUuid("00000000-0000-0000-0000-000000000000");
			notification.setMessage("Personen blev fysisk slettet fra systemet, efter at have været inaktiv siden: " + person.getLastChanged());
			notification.setCreated(new Date());
			notification.setNotificationType(NotificationType.PERSON_PERMANENTLY_DELETED);
			notificationService.save(notification);
		}
	}

	public Person getLoggedInPerson() {
		String userId = SecurityUtil.getUser();
		if (userId != null) {
			User user = userService.findByUserIdAndUserType(userId, SupportedUserTypeService.getActiveDirectoryUserType());

			if (user != null) {
				return findByUser(user);
			}
		}

		return null;
	}

	public List<Person> getByAffiliationMasters(List<String> affiliationMasters) {
		return null;
	}

	public static Person getManager(Person person, String employeeId) {
		Affiliation affiliation = getAffiliation(person, employeeId);

		if (affiliation != null) {
			if (affiliation.getCalculatedOrgUnit() != null &&
				affiliation.getCalculatedOrgUnit().getManager() != null &&
				affiliation.getCalculatedOrgUnit().getManager().getManager() != null) {

				return affiliation.getCalculatedOrgUnit().getManager().getManager();
			}
		}

		return null;
	}

	public static OrgUnitManager getOrgUnitManager(Person person, String employeeId) {
		Affiliation affiliation = getAffiliation(person, employeeId);

		if (affiliation != null) {
			if (affiliation.getCalculatedOrgUnit() != null &&
					affiliation.getCalculatedOrgUnit().getManager() != null &&
					affiliation.getCalculatedOrgUnit().getManager().getManager() != null) {

				return affiliation.getCalculatedOrgUnit().getManager();
			}
		}

		return null;
	}

	// we use this method in case the person is the manager himself
	public record ManagerFromPersonResponse(OrgUnitManager manager, Affiliation relatedAffiliation) { }
	public static ManagerFromPersonResponse getManagerDifferentFromPerson(Person person, String employeeId) {
		Affiliation affiliation = getAffiliation(person, employeeId);

		if (affiliation != null) {
			if (affiliation.getCalculatedOrgUnit() != null
					&& affiliation.getCalculatedOrgUnit().getManager() != null
					&& affiliation.getCalculatedOrgUnit().getManager().getManager() != null) {

				OrgUnitManager orgUnitManager = affiliation.getCalculatedOrgUnit().getManager();

				if (!Objects.equals(orgUnitManager.getManager().getUuid(), person.getUuid())) {
					return new ManagerFromPersonResponse(orgUnitManager, affiliation);
				}

				if (orgUnitManager.getOrgUnit().getParent() == null) {
					return null;
				}

				OrgUnitManager parentManager = orgUnitManager.getOrgUnit().getParent().getManager();
				if (parentManager != null && !(Objects.equals(parentManager.getManager().getUuid(), person.getUuid()))) {
					return new ManagerFromPersonResponse(parentManager, affiliation);
				} else {
					//find different manager all the way up the hierarchy
					OrgUnit parentOrgUnit = orgUnitManager.getOrgUnit().getParent().getParent();
					while (parentOrgUnit != null) {
						parentManager = parentOrgUnit.getManager();
						if (parentManager != null && !(Objects.equals(parentManager.getManager().getUuid(), person.getUuid()))) {
							// success found different manager
							return new ManagerFromPersonResponse(parentManager, affiliation);
						} else {
							parentOrgUnit = parentOrgUnit.getParent();
						}
					}
				}
			}
		}

		return null;
	}

	private static Affiliation getAffiliation(Person person, String employeeId) {
		Affiliation affiliation = null;

		if (employeeId != null) {
			for (Affiliation aff : person.getAffiliations()) {
				if (aff.getEmployeeId() != null && aff.getEmployeeId().equals(employeeId)) {
					affiliation = aff;
					break;
				}
			}
		}
		else {
			// just pick the primary affiliation
			Optional<Affiliation> oAffiliation = person.getAffiliations().stream().filter(a -> a.isPrime()).findFirst();
			affiliation = (oAffiliation.isPresent()) ? oAffiliation.get() : null;

			// special case - we are not looking for an affiliation tied to a specific employment,
			// but the person has no prime affiliation (most likely cause, the person has no active
			// affiliations), so we find the affiliation that was stopped last, and use that.
			//
			// This is useful for deactivating account based on all-stopped affiliations, and the need
			// to notify the previous manager
			if (affiliation == null) {
				Affiliation latestStoppedAffiliation = null;

				for (Affiliation a : person.getAffiliations()) {
					if (!a.isDeleted() && a.getStopDate() != null) {
						if (latestStoppedAffiliation != null) {
							if (latestStoppedAffiliation.getStopDate().before(a.getStopDate())) {
								latestStoppedAffiliation = a;
							}
						}
						else {
							latestStoppedAffiliation = a;
						}
					}
				}

				affiliation = latestStoppedAffiliation;
			}
		}
		return affiliation;
	}

	public List<Person> findAllSofdSubstitutesForManager(Person manager, String employeeId) {
		List<Person> substitutes = new ArrayList<>();

		for (SubstituteAssignment substituteAssignment : manager.getSubstitutes()) {
			boolean validSubstitute = false;

			if (Objects.equals("GLOBAL", substituteAssignment.getContext().getIdentifier())) {
				validSubstitute = true;
			}
			// SOFD affiliations can be data-constrained, so we need to validate a bit
			else if (Objects.equals("SOFD", substituteAssignment.getContext().getIdentifier())) {
				if (employeeId == null || substituteAssignment.getConstraintMappings().size() == 0) {
					validSubstitute = true;
				}
				else {
					List<Affiliation> affiliations = affiliationService.getByEmployeeId(employeeId);

					for (Affiliation affiliation : affiliations) {
						for (SubstituteAssignmentOrgUnitMapping constraint : substituteAssignment.getConstraintMappings()) {
							if (Objects.equals(affiliation.getCalculatedOrgUnit().getUuid(), constraint.getOrgUnit().getUuid())) {
								validSubstitute = true;
								break;
							}
						}

						if (validSubstitute) {
							break;
						}
					}
				}
			}

			if (validSubstitute) {
				substitutes.add(substituteAssignment.getSubstitute());
			}
		}

		return substitutes;
	}

	@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
	public void handlePersonsOnLeave() {
		Date now = new Date();
		List<Person> persons = personDao.findByLeaveNotNull();

		SecurityUtil.fakeLoginSession();

		for (Person person : persons) {
			if (person.getLeave().getStopDate() == null) {
				continue;
			}

			Calendar cal = Calendar.getInstance();
			cal.setTime(person.getLeave().getStopDate());
			cal.add(Calendar.HOUR_OF_DAY, 23);
			cal.add(Calendar.MINUTE, 59);
			cal.add(Calendar.SECOND, 59);
			Date stopDate = cal.getTime();

			if (now.after(stopDate)) {
				List<AccountOrder> orders = removeLeave(person);
				for (AccountOrder order : orders) {
					accountOrderService.save(order);
				}

				self.save(person);
			}
		}
	}

	private LocalDate toLocalDate(Date date) {
		if (date == null) {
			return null;
		}

		// might be an SQL instance, so convert to something that has a toInstant() method on it
		if (date instanceof java.sql.Date) {
			date = new Date(date.getTime());
		}

	    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}

	@Transactional
	public void expiryReminder() {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		EmailTemplate reminder = emailTemplateService.findByTemplateType(EmailTemplateType.AFFILIATION_EXPIRE_REMINDER);

		for (EmailTemplateChild child : reminder.getChildren()) {
			if (child.isEnabled()) {
				Date inXDays = getDateOffsetDays((int) child.getDaysBeforeEvent());
				LocalDate inXDaysLocal = toLocalDate(inXDays);

				List<Person> personsWithAffiliationExpireX = personDao.findByAffiliationsMasterAndAffiliationsStopDate("SOFD", inXDays);

				for (Person person : personsWithAffiliationExpireX) {
					for (Affiliation affiliation : person.getAffiliations()) {
						if (!"SOFD".equals(affiliation.getMaster())) {
							continue;
						}

						if (affiliation.getStopDate() == null || !inXDaysLocal.isEqual(toLocalDate(affiliation.getStopDate()))) {
							continue;
						}

						if (configuration.getEmailTemplate().isOrgFilterEnabled() && reminder.getTemplateType().isShowOrgFilter()) {
							List<String> excludedOUUuids = child.getExcludedOrgUnitMappings().stream().map(o -> o.getOrgUnit()).map(o -> o.getUuid()).collect(Collectors.toList());
							if (excludedOUUuids.contains(affiliation.getCalculatedOrgUnit().getUuid())) {
								log.info("Not sending email for email template child with id " + child.getId() + " for affiliation with uuid " + affiliation.getUuid() + ". The affiliation OU was in the excluded ous list");
								continue;
							}
						}

						processAffiliation(child, dateFormat, person, affiliation);
					}
				}
			}
		}
	}

	private void processAffiliation(EmailTemplateChild templateChild, DateFormat dateFormat, Person person, Affiliation affiliation) {
		var managerResponse = PersonService.getManagerDifferentFromPerson(affiliation.getPerson(), affiliation.getEmployeeId());
		if (managerResponse == null) {
			log.warn("ProcessAffiliation - OrgUnit: " + affiliation.getCalculatedOrgUnit().getUuid() + " doesn't have a manager.");
			return;
		}

		List<Person> recipients = emailTemplateService.getManagerOrSubstitutes(templateChild, managerResponse.manager().getManager(), affiliation.getCalculatedOrgUnit().getUuid());

		if (CollectionUtils.isEmpty(recipients)) {
			log.warn("ProcessAffiliation - no email address found.");
			return;
		}

		String userId = getUserIdForAffiliation(affiliation);

		String message = templateChild.getMessage()
				.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), getName(managerResponse.manager().getManager()))
				.replace(EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER.getPlaceholder(), getName(person))
				.replace(EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER.getPlaceholder(), affiliation.getCalculatedOrgUnit().getName())
				.replace(EmailTemplatePlaceholder.TIMESTAMP_PLACEHOLDER.getPlaceholder(), dateFormat.format(affiliation.getStopDate()))
				.replace(EmailTemplatePlaceholder.VENDOR_PLACEHOLDER.getPlaceholder(), (affiliation.getVendor() != null) ? affiliation.getVendor() : "")
				.replace(EmailTemplatePlaceholder.INTERNAL_REFERENCE_PLACEHOLDER.getPlaceholder(), (affiliation.getInternalReference() != null) ? affiliation.getInternalReference() : "")
				.replace(EmailTemplatePlaceholder.DAYS_BEFORE_EVENT.getPlaceholder(), "" + templateChild.getDaysBeforeEvent())
				.replace(EmailTemplatePlaceholder.POSITION_NAME_PLACEHOLDER.getPlaceholder(), AffiliationService.getPositionName(affiliation))
				.replace(EmailTemplatePlaceholder.EMPLOYEE_NUMBER_PLACEHOLDER.getPlaceholder(), affiliation.getEmployeeId() != null ? affiliation.getEmployeeId() : "")
				.replace(EmailTemplatePlaceholder.ACCOUNT_PLACEHOLDER.getPlaceholder(), userId);

		String title = templateChild.getTitle()
				.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), getName(managerResponse.manager().getManager()))
				.replace(EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER.getPlaceholder(), getName(person))
				.replace(EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER.getPlaceholder(), affiliation.getCalculatedOrgUnit().getName())
				.replace(EmailTemplatePlaceholder.TIMESTAMP_PLACEHOLDER.getPlaceholder(), dateFormat.format(affiliation.getStopDate()))
				.replace(EmailTemplatePlaceholder.VENDOR_PLACEHOLDER.getPlaceholder(), (affiliation.getVendor() != null) ? affiliation.getVendor() : "")
				.replace(EmailTemplatePlaceholder.INTERNAL_REFERENCE_PLACEHOLDER.getPlaceholder(), (affiliation.getInternalReference() != null) ? affiliation.getInternalReference() : "")
				.replace(EmailTemplatePlaceholder.DAYS_BEFORE_EVENT.getPlaceholder(), "" + templateChild.getDaysBeforeEvent())
				.replace(EmailTemplatePlaceholder.POSITION_NAME_PLACEHOLDER.getPlaceholder(), AffiliationService.getPositionName(affiliation))
				.replace(EmailTemplatePlaceholder.EMPLOYEE_NUMBER_PLACEHOLDER.getPlaceholder(), affiliation.getEmployeeId() != null ? affiliation.getEmployeeId() : "")
				.replace(EmailTemplatePlaceholder.ACCOUNT_PLACEHOLDER.getPlaceholder(), userId);

		for (Person recipient : recipients) {
			emailService.sendMessage(PersonService.getEmail(recipient), title, message, null, null, templateChild);
		}
	}

	private String getUserIdForAffiliation(Affiliation affiliation) {
		String userId = "";
		List<User> users = affiliation.getPerson().getUsers().stream().map(u -> u.getUser()).collect(Collectors.toList());
		// first try to find a user that is specifically mapped to given affiliation
		Optional<User> user = users.stream().filter(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType()) && Objects.equals(u.getEmployeeId(), affiliation.getEmployeeId())).findAny();
		if (user.isPresent()) {
			userId = user.get().getUserId();
		} else {
			// then try to find a user that is prime, but not mapped to a specific affiliation
			Optional<User> primeUser = users.stream().filter(u -> !StringUtils.hasLength(u.getEmployeeId()) && SupportedUserTypeService.isActiveDirectory(u.getUserType()) && u.isPrime()).findAny();
			if (primeUser.isPresent()) {
				userId = primeUser.get().getUserId();
			}
		}
		return userId;
	}

	private Date getDateOffsetDays(int days) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.DAY_OF_YEAR, days);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);

		return cal.getTime();
	}

	public static String maskCpr(String cpr) {
		boolean userHasCPRAccess = SecurityUtil.getUserRoles().contains(RoleConstants.USER_ROLE_CPR_ACCESS);
		if (userHasCPRAccess) {
			return cpr;
		}

		if (cpr != null && cpr.length() > 6) {
			return cpr.substring(0, 6) + "-XXXX";
		}

		return "";
	}

	public List<AccountOrder> removeLeave(Person person) {
		List<AccountOrder> newOrders = new ArrayList<AccountOrder>();

		// safety valve
		if (person.getLeave() == null) {
			return newOrders;
		}

		if (person.getLeave().isExpireAccounts()) {
			// remove any existing expire orders
			accountOrderService.deletePendingExpireOrders(person);

			// generate fresh expire orders
			newOrders = generateExpireOrders(person, new Date(), null);
		}

		// if the disable flag was set during leave, then remove it
		if (person.getLeave().isDisableAccountOrders()) {
			person.setDisableAccountOrders(false);
		}

		// bye bye leave flag
		person.setLeave(null);

		return newOrders;
	}

	public List<AccountOrder> generateReactivateOrders(Person person, String[] accountsToReactivate) {
		List<AccountOrder> newOrders = new ArrayList<AccountOrder>();

		List<AccountOrder> pendingOrders = accountOrderService.getPendingOrders(person);
		List<User> deactivatedUsers = PersonService.getUsers(person).stream().filter(u -> u.isDisabled() == true).collect(Collectors.toList());

		for (String accountToReactivate : accountsToReactivate) {
			// if the original string is empty, we get a single empty entry, other than that we are assured valid data
			if (accountToReactivate.length() == 0) {
				continue;
			}

			// remove any pending deactivate and delete orders for this type
			for (AccountOrder order : pendingOrders) {
				if (!order.getUserType().equals(accountToReactivate)) {
					continue;
				}

				switch (order.getOrderType()) {
					case DEACTIVATE:
					case DELETE:
						accountOrderService.delete(order);
						break;
					case CREATE:
					case EXPIRE:
						// ignore
						break;
				}
			}

			// reactivate any deactivated accounts for this user of this type
			for (User user : deactivatedUsers) {
				if (!accountToReactivate.equals(user.getUserType())) {
					continue;
				}

				// if there already is a pending order to reactivate this account, skip
				boolean skip = false;
				for (AccountOrder order : pendingOrders) {
					if (!order.getOrderType().equals(AccountOrderType.CREATE)) {
						continue;
					}

					if (!order.getUserType().equals(user.getUserType())) {
						continue;
					}

					if (!Objects.equals(order.getRequestedUserId(), user.getUserId())) {
						continue;
					}

					// we have a CREATE (reactivate) order for this specific account, so skip
					skip = true;
				}

				if (skip) {
					continue;
				}

				SupportedUserType supportedUserType = supportedUserTypeService.findByKey(user.getUserType());
				AccountOrder order = accountOrderService.createAccountOrder(
						person,
						supportedUserType,
						user.getUserId(),
						null,
						user.getEmployeeId(),
						new Date(),
						EndDate.NO,
						null,
						false,
						configuration.getModules().getAccountCreation().isForceSetEmployeeId(),
						false);

				newOrders.add(order);
			}
		}

		return newOrders;
	}

	public List<AccountOrder> generateExpireOrders(Person person, Date activationDate, Date expireDate) {
		if (expireDate != null) {
			// move the supplied timestamp to mid-day - our agent will make sure the actual
			// time is set to the beginning of that day (but timezone issues can be a problem
			// when sending the TTS through JSON/REST over timezones)
			Calendar cal = Calendar.getInstance();
			cal.setTime(expireDate);
			cal.set(Calendar.HOUR, 12);

			expireDate = cal.getTime();
		}

		List<AccountOrder> orders = new ArrayList<>();

		List<User> activeADUsers = PersonService.getUsers(person).stream()
			.filter(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType()))
			.filter(u -> u.isDisabled() == false)
			.collect(Collectors.toList());

		for (User user : activeADUsers) {
			AccountOrder order = new AccountOrder();
			order.setActivationTimestamp(activationDate);
			order.setEndDate(expireDate);
			order.setOrderedTimestamp(new Date());
			order.setOrderType(AccountOrderType.EXPIRE);
			order.setPersonUuid(person.getUuid());
			order.setRequestedUserId(user.getUserId());
			order.setStatus(AccountOrderStatus.PENDING);
			order.setUserType(user.getUserType());

			orders.add(order);
		}

		return orders;
	}

	public List<AccountOrder> generateDeactivateOrders(Person person, String[] accountsToDeactivate, Date startDate) {
		List<AccountOrder> newOrders = new ArrayList<AccountOrder>();

		List<User> activeUsers = PersonService.getUsers(person).stream().filter(u -> u.isDisabled() == false).collect(Collectors.toList());

		for (String accountToDeactivate : accountsToDeactivate) {
			// split can have empty entries *sigh*
			if (accountToDeactivate.length() == 0) {
				continue;
			}

			for (User user : activeUsers) {
				if (!accountToDeactivate.equals(user.getUserType())) {
					continue;
				}

				// only generate deactive orders if deactivate is enabled
				SupportedUserType supportedUserType = supportedUserTypeService.findByKey(user.getUserType());
				if (supportedUserType == null || supportedUserType.isCanOrder() == false || supportedUserType.getDaysToDeactivate() == 0) {
					continue;
				}

				AccountOrder order = accountOrderService.deactivateOrDeleteAccountOrder(AccountOrderType.DEACTIVATE, person, user.getEmployeeId(), user.getUserType(), user.getUserId(), startDate);
				newOrders.add(order);
			}
		}

		return newOrders;
	}

	public boolean hasNotStoppedAffiliationInWagesSystem(Person person) {
		return AffiliationService.notStoppedAffiliations(person.getAffiliations()).stream().anyMatch(aff -> Objects.equals(aff.getMaster(), sofdConfiguration.getModules().getLos().getPrimeAffiliationMaster()));
	}

	public boolean hasAffiliationInWagesSystem(Person person) {
		return person.getAffiliations().stream().anyMatch(aff -> Objects.equals(aff.getMaster(), sofdConfiguration.getModules().getLos().getPrimeAffiliationMaster()));
	}

	// 10 days after the last affiliation has been stopped, remove all flags
	@Transactional
	public void cleanupDeletedFlag() {
		Calendar c1 = Calendar.getInstance();
		c1.setTime(new Date());
		c1.add(Calendar.DATE, -10);
		Date date10daysAgo = c1.getTime();

		List<Person> persons = personDao.findByForceStopTrueOrDisableAccountOrdersTrueOrLeaveNotNull();
		if (persons.isEmpty()) {
			return;
		}

		SecurityUtil.fakeLoginSession();

		for (Person person : persons) {
			List<Affiliation> affiliations = person.getAffiliations();

			if (affiliations.stream().allMatch(aff -> aff.getStopDate() != null && aff.getStopDate().before(date10daysAgo))) {
				person.setForceStop(false);
				person.setDisableAccountOrders(false);
				person.setLeave(null);

				self.save(person);
			}
		}
	}

	public List<String> getUuidsOfDisableAccountOrderPersons() {
		return personDao.getUuidsOfDisableAccountOrderPersons();
	}

	public void setEmployeeId(Person person, User user, String employeeId, LocalDate date) throws Exception {
		// only allow updates for AD users
		if (SupportedUserTypeService.isActiveDirectory(user.getUserType())) {

			// start by removing any existing pending future changes
			UserChangeEmployeeIdQueue queue = userChangeEmployeeIdQueueService.findByUser(user);
			if (queue != null) {
				userChangeEmployeeIdQueueService.delete(queue);
			}

			// los and sofd affiliations
			if (person.getAffiliations().stream().anyMatch(a -> Objects.equals(a.getEmployeeId(), employeeId) )) {
				if (date != null) {
					if (date.isEqual(java.time.LocalDate.now())) {
						user.setEmployeeId(employeeId);
					}
					else if (date.isAfter(java.time.LocalDate.now())){
						queue = new UserChangeEmployeeIdQueue();
						queue.setDateOfTransaction(date);
						queue.setEmployeeId(employeeId);
						queue.setUser(user);

						userChangeEmployeeIdQueueService.save(queue);
					}
					else {
						throw new Exception("Effektueringsdatoen må ikke være før dags dato");
					}
				}
				else {
					user.setEmployeeId(employeeId);
				}
			}
			else {
				user.setEmployeeId(null);
			}
			self.save(person);
		}
	}

	public static boolean isFictionalCpr(String cpr) {
		try {
			LocalDate.parse(cpr.substring(0, 6), DateTimeFormatter.ofPattern("ddMMyy"));
		}
		catch (DateTimeParseException ex) {
			return true;
		}

		return false;
	}

	public AutoCompleteResult substituteSearchPerson(String term, String uuid, String ous) {
		List<Person> persons = null;

		// scenario from API where we limit person search to only those in given orgUnits with other managers
		if (ous != null) {
			List<String> ouUuids = Arrays.asList(ous.split(","));
			List<OrgUnit> orgUnitsIncludingChildren = orgUnitService.getAllWithChildren(ouUuids);

			// get active affiliations from given orgUnits
			List<Affiliation> affiliations = new ArrayList<>();
			List<List<Affiliation>> affiliationsLists = orgUnitsIncludingChildren.stream().map(o -> o.getAffiliations()).collect(Collectors.toList());
			for (List<Affiliation> affiliationsList : affiliationsLists) {
				affiliations.addAll(AffiliationService.onlyActiveAffiliations(affiliationsList));
			}

			// extract persons from affiliations
			persons = affiliations.stream().map(aff -> aff.getPerson()).collect(Collectors.toList());

			// add other managers
			List<Person> allManagers = orgUnitService.getAll().stream().map(ou -> ou.getManager()).filter(m -> m != null).map(m -> m.getManager()).collect(Collectors.toList());
			persons.addAll(allManagers);

			// filter persons by given search term and not deleted
			persons = persons
					.stream().filter(p -> !p.isDeleted() && getName(p).toLowerCase().contains(term.toLowerCase())).distinct()
					.collect(Collectors.toList());
		} else {
			persons = personDao.findTop10ByName(term);
		}

		persons.removeIf(p -> p.getUuid().equals(uuid));

		List<ValueData> suggestions = new ArrayList<>();
		for (Person person : persons) {
			StringBuilder builder = new StringBuilder();
			builder.append(PersonService.getName(person));

			Optional<Affiliation> primeAffiliation = person.getAffiliations().stream().filter(a -> a.isPrime()).findFirst();
			if (primeAffiliation.isPresent()) {
				builder.append(" (" + primeAffiliation.get().getCalculatedOrgUnit().getName() + ")"); // TODO: ok
			}

			ValueData vd = new ValueData();
			vd.setValue(builder.toString());
			vd.setData(person.getUuid());

			suggestions.add(vd);
		}

		AutoCompleteResult result = new AutoCompleteResult();
		result.setSuggestions(suggestions);
		return result;
	}

	@Transactional(rollbackFor = Exception.class)
	public void cleanupOldActiveDirectoryData() {
		SecurityUtil.fakeLoginSession();
		List<Person> persons = getActive();

		for (Person person : persons) {
			if (person.getUsers().stream().noneMatch(um -> SupportedUserTypeService.isActiveDirectory(um.getUser().getUserType()) && um.getUser().isPrime())) {
				photoService.delete(person.getUuid());

				if (person.getPhones().removeIf(phoneMapping -> Objects.equals(phoneMapping.getPhone().getMaster(), "ACTIVE_DIRECTORY"))) {
					self.save(person);
				}
			}
		}
	}

	public void deleteUserByADMasterId(String masterId) {
		var persons = personDao.findByUsersUserUserTypeAndUsersUserMasterId(SupportedUserTypeService.getActiveDirectoryUserType(), masterId);

		for (var person : persons) {
			var userIdsToBeRemoved = new ArrayList<Long>();

			for (var user : person.getUsers()) {

				// check if user is an AD user and matches input master id
				if (user.getUser().getMaster().equalsIgnoreCase("ActiveDirectory") &&
					SupportedUserTypeService.isActiveDirectory(user.getUser().getUserType()) &&
					user.getUser().getMasterId().equalsIgnoreCase(masterId)) {

					// flag this user for removal
					userIdsToBeRemoved.add(user.getId());

					// also find any existing exchange user based on this ad user
					var exchangeUser = person.getUsers().stream()
							.filter(u -> u.getUser().getMaster().equalsIgnoreCase("ActiveDirectory") &&
										 SupportedUserTypeService.isExchange(user.getUser().getUserType()) &&
										 u.getUser().getMasterId().equalsIgnoreCase(user.getUser().getUserId()))
							.findFirst()
							.orElse(null);

					if (exchangeUser != null) {
						userIdsToBeRemoved.add(exchangeUser.getId());
					}
				}
			}

			if (!userIdsToBeRemoved.isEmpty()) {
				person.getUsers().removeIf(u -> userIdsToBeRemoved.contains(u.getUser().getId()));
				self.save(person);
			}
		}
	}
}
