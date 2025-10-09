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
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import dk.digitalidentity.sofd.dao.PersonDaoCustom;
import dk.digitalidentity.sofd.dao.model.enums.LeaveReason;
import dk.digitalidentity.sofd.dao.model.mapping.PersonUserMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.history.Revision;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import dk.digitalidentity.sofd.config.RoleConstants;
import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.dto.history.HistoryPerson;
import dk.digitalidentity.sofd.dao.OrgUnitManagerDao;
import dk.digitalidentity.sofd.dao.PersonDao;
import dk.digitalidentity.sofd.dao.ReservedUsernameDao;
import dk.digitalidentity.sofd.dao.model.AccountOrder;
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
	private PersonDaoCustom personDaoCustom;

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
	private SofdConfiguration configuration;

	@Autowired
	private UserChangeEmployeeIdQueueService userChangeEmployeeIdQueueService;

	@Autowired
	private PhotoService photoService;

	@Autowired
	private OrgUnitService orgUnitService;

	@Autowired
	private SettingService settingService;

	@Autowired
	private OrgUnitManagerDao orgUnitManagerDao;

	@Autowired
	private ModificationHistoryService modificationHistoryService;

	@Autowired
	private ReservedUsernameDao reservedUsernameDao;
	
	@Autowired
	private PrimeService primeService;
	
	@Autowired
	private EmailTemplateChildService emailTemplateChildService;
	
	@Autowired
	private EmailQueueService emailQueueService;

	@Autowired
	private SubstituteOrgUnitAssignmentService substituteOrgUnitAssignmentService;

	@Autowired
	private ObjectMapper mapper;


	public boolean isManager(Person person) {
		return orgUnitManagerDao.existsByManagerUuid(person.getUuid());
	}

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
	
	public List<Person> getByOffsetAndLimit(String offset, int size) {
		if (StringUtils.hasText(offset)) {
			return personDao.findLimitedWithOffset(size, offset);
		}

		return personDao.findLimited(size);
	}

	public List<Person> getAll() {
		return personDao.findAll();
	}

	public List<Person> getActive() {
		return personDao.findByDeletedFalse();
	}

	@Transactional
	public List<Person> getActive(Consumer<Person> consumer) {
		List<Person> persons = personDao.findByDeletedFalse();
		
		if (consumer != null) {
			persons.forEach(consumer);
			
    		// needed by AbstractBeforeSaveInterceptor
			persons.forEach(p -> {
	    		p.getPhones().forEach(ph -> ph.getPhone().getPhoneNumber());
	    		p.getAffiliations().forEach(a -> {
	    			a.getWorkplaces().forEach(w -> w.getOrgUnit().getUuid());
	    			a.getOrgUnit().getUuid();

	    			OrgUnit unit = a.getCalculatedOrgUnit();
	    			if (unit != null) {
	    				unit.getCreated();
	    			}
	    		});
	    		p.getUsers().forEach(u -> {
	    			if (u.getUser().getActiveDirectoryDetails() != null) {
	    				u.getUser().getActiveDirectoryDetails().getKombitUuid();
	    			}
	    		});
	    		if (p.getRegisteredPostAddress() != null) {
	    			p.getRegisteredPostAddress().getCity();
	    		}
	    		if (p.getResidencePostAddress() != null) {
	    			p.getResidencePostAddress().getCity();
	    		}
			});
		}
		
		return persons;
	}

	public List<Person> getByPhoneMasterAndMasterId(String master, String masterId) {
		return personDao.findByPhonesPhoneMasterAndPhonesPhoneMasterId(master, masterId);
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
		// return orgUnitManagerDao.findByInheritedFalse();

		// if we do not return them all - we miss information about substitutes in the UI, if the substitutes are constrainted to inherited OU's
		return orgUnitManagerDao.findAll();
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
		person.setLastChanged();
		return personDao.save(person);
	}

	public void deleteExistingDuplicateUsers(Person person) {
		var personsWithDuplicateUsers = personDaoCustom.findPersonsWithDuplicateUsers(person);
		for (var personWithDuplicateUsers : personsWithDuplicateUsers) {
			for (var conflictingUser : person.getUsers())
			{
				Iterator<PersonUserMapping> iterator = personWithDuplicateUsers.getUsers().iterator();
				while (iterator.hasNext()) {
					var u = iterator.next();
					if (Objects.equals(u.getUser().getMaster(), conflictingUser.getUser().getMaster())
							&& Objects.equals(u.getUser().getMasterId(), conflictingUser.getUser().getMasterId())) {
						log.info("Deleting duplicate user with master " + u.getUser().getMaster() + " and masterId " + u.getUser().getMasterId() + " from person with uuid " + personWithDuplicateUsers.getUuid());
						iterator.remove();
					}
				}
			}
			self.save(personWithDuplicateUsers);
		}
	}

	@Transactional
	public void saveBulkWithTransaction(List<Person> persons) {
		for (Person person : persons) {
			self.save(person);
		}
	}

	// TODO: actually implement our interceptors to take care of the saveAll() method, so we can get rid of this hack :)
	//
	// please don't use this method - it bypasses all our save interceptors - so only use it in places where we don't need our interceptors
	// to run, which is basically nowhere (except for MitID import, because Brian says so ;))
	// and NEVER use this for creating new persons, only for updates....
	@Deprecated
	public void saveAll(List<Person> persons) {
		// perform a mini-intercept, just to make sure the basics are covered
		for (Person person : persons) {
			if (person.getUuid() == null) {
				throw new RuntimeException("Do not use saveAll() for creating persons - actually never use this method!");
			}

			// always needed
			person.setLastChanged();
			
			// to make sure any MitID Erhverv accounts are flagged as prime where needed
			if (person.getUsers() != null) {
				for (String userType : supportedUserTypeService.getAllUserTypes()) {
					List<User> users = PersonService.getUsers(person).stream().filter(u -> u.getUserType().equals(userType)).collect(Collectors.toList());
					
					if (users != null && users.size() > 0) {
						primeService.setPrimeUser(users);
					}
				}
			}

		}
		
		personDao.saveAll(persons);
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

	public static User getPrimaryActivDirectoryUser(Person person) {
		return person.getUsers().stream()
				.map(m -> m.getUser())
				.filter(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType()) && u.isPrime())
				.findFirst()
				.orElse(null);
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

	// all other prime flags are automatically set when calling save(), but
	// both users and affiliations have an expire-date, so we need to check
	// that daily, and update the prime flag in case the prime field is expired
	@Transactional(rollbackFor = Exception.class)
	public void setPrimeAffiliationPrimeUserAndDeleted() {
		log.info("Executing setPrimeAffiliationPrimeUserAndDeleted task");

		SecurityUtil.fakeLoginSession();

		List<Person> persons = getAll();
		List<String> allUserTypes = supportedUserTypeService.getAllUserTypes();
		List<Person> toSave = new ArrayList<>();
		
		int counter = 0, saveCounter = 0;
		for (Person person : persons) {
			counter ++;
			if (counter % 500 == 0) {
				log.info("Handling person " + counter + " of " + persons.size());
			}

			boolean changes = primeService.setPrimeAffilation(person);
			
			if (person.getUsers() != null) {
				for (String userType : allUserTypes) {
					List<User> users = PersonService.getUsers(person).stream().filter(u -> u.getUserType().equals(userType)).collect(Collectors.toList());
					
					if (users != null && users.size() > 0) {
						changes = changes || primeService.setPrimeUser(users);
					}
				}
			}
			
			// if the person does not have any users, and does not have any prime affiliations (i.e. no active affiliations), then flip the delete flag
			boolean shouldBeDeleted = (person.getUsers().size() == 0 && !person.getAffiliations().stream().anyMatch(a -> a.isPrime()));
			if (shouldBeDeleted && !person.isDeleted()) {
				person.setDeleted(true);
				changes = true;
			}
			else if (!shouldBeDeleted && person.isDeleted()) {
				person.setDeleted(false);
				changes = true;
			}

			if (changes) {
				saveCounter++;
				person.setLastChanged();
				toSave.add(person);
			}
		}

		// bypass interceptors - which is okay as we handle consistency here, and also set the lastChanged field
		if (toSave.size() > 0) {
			personDao.saveAll(toSave);
		}

		log.info("Finished executing setPrimeAffiliationPrimeUserAndDeleted task - with " + saveCounter + " saves");
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

		if (post != null) {
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
			if (!dead) {
				// I want a trace of this - not sure why this happeneded, but we want to know about it, and what triggered it
				log.error("Blocking resurrecting of " + person.getUuid(), new RuntimeException());
			}
			else {
				person.setDead(dead);
	
				if (dead) {
					sendDeadOrDisenfranchisedNotification(person, NotificationType.PERSON_DEAD, "Personen er blevet meldt død eller bortkommet i cpr registeret");
	
					if( person.getUsers().stream().anyMatch(u -> SupportedUserTypeService.isActiveDirectory(u.getUser().getUserType())) ) {
						sendDeadOrDisenfranchisedNotification(person, NotificationType.PERSON_DEAD_AD_ONLY, "Personen er registreret med en AD-konto og er blevet meldt død eller bortkommet i cpr registeret");
					}
					if( person.getUsers().stream().anyMatch(u -> !u.getUser().isDisabled() && SupportedUserTypeService.isActiveDirectory(u.getUser().getUserType())) ) {
						sendDeadOrDisenfranchisedNotification(person, NotificationType.PERSON_DEAD_ACTIVE_AD_ONLY, "Personen er registreret med en aktiv AD-konto og er blevet meldt død eller bortkommet i cpr registeret");
					}
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
				if( person.getUsers().stream().anyMatch(u -> !u.getUser().isDisabled() && SupportedUserTypeService.isActiveDirectory(u.getUser().getUserType())) ) {
					sendDeadOrDisenfranchisedNotification(person, NotificationType.PERSON_DISENFRANCHISED_ACTIVE_AD_ONLY, "Personen er registreret med en aktiv AD-konto og er blevet meldt umyndiggjort i cpr registeret");
				}
			};
		}

		if (dead || disenfranchised) {

			// set expiry on all not disabled AD accounts
			List<AccountOrder> newOrders = new ArrayList<AccountOrder>();
			List<AccountOrder> orders = generateExpireOrders(person, new Date(), new Date(), null, null, null, null, null);
			newOrders.addAll(orders);

			person.setDisableAccountOrdersCreate(true);

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

	public static List<Person> getAllManagers(Person person) {
		List<Person> allManagers = new ArrayList<>();
		for( Affiliation affiliation : AffiliationService.notStoppedAffiliations(person.getAffiliations())) {
			if (affiliation.getCalculatedOrgUnit() != null &&
					affiliation.getCalculatedOrgUnit().getManager() != null &&
					affiliation.getCalculatedOrgUnit().getManager().getManager() != null) {

				allManagers.add(affiliation.getCalculatedOrgUnit().getManager().getManager());
			}
		}
		return allManagers;
	}

	public static Person getManager(Affiliation affiliation, Person person, String employeeId) {
		var orgUnitManager = getOrgUnitManager(affiliation, person, employeeId);
		return orgUnitManager != null ? orgUnitManager.getManager() : null;
	}

	public static OrgUnitManager getOrgUnitManager(Affiliation affiliation, Person person, String employeeId) {
		// if no affiliation is specified we fall back to getting the manager using person and employeeId
		if (affiliation == null ) {
			affiliation = getAffiliation(person, employeeId);
		}

		if (affiliation != null) {
			if (affiliation.getCalculatedOrgUnit() != null &&
				affiliation.getCalculatedOrgUnit().getManager() != null &&
				affiliation.getCalculatedOrgUnit().getManager().getManager() != null) {
				return affiliation.getCalculatedOrgUnit().getManager();
			}
		}

		return null;
	}

	// finds persons that are valid as managers
	public List<Person> searchValidMangers(String query) {
		return personDao.findTop10ValidManagersByName(query);
	}

	public List<Person> searchPersons(String query, boolean cprAccess) {
		return personDao.searchPersons(query, cprAccess);
	}

	public List<Person> findByCPRStartingWith(String query) {
		return personDao.findTop10ByCprStartingWith(query);
	}

	public void migrateFictiveCpr() {

		Authentication authentication = SecurityUtil.getLoginSession();
		try {
			SecurityUtil.fakeLoginSession();
			var persons = personDao.findAll();
			for( var person : persons)
			{
				if(person.isFictiveCpr() != PersonService.isFictionalCpr(person.getCpr())) {
					// the setCpr method will update fictiveCpr
					person.setCpr(person.getCpr());
					personDao.save(person);
				}
			}
		}
		finally {
			SecurityUtil.setLoginSession(authentication);
		}
	}

	// we use this method in case the person is the manager himself
	public static OrgUnitManager getManagerDifferentFromPerson(Person person, Affiliation affiliation) {
		if (affiliation != null) {
			if (affiliation.getCalculatedOrgUnit() != null
					&& affiliation.getCalculatedOrgUnit().getManager() != null
					&& affiliation.getCalculatedOrgUnit().getManager().getManager() != null) {

				OrgUnitManager orgUnitManager = affiliation.getCalculatedOrgUnit().getManager();

				if (!Objects.equals(orgUnitManager.getManager().getUuid(), person.getUuid())) {
					return orgUnitManager;
				}

				if (orgUnitManager.getOrgUnit().getParent() == null) {
					return null;
				}

				OrgUnitManager parentManager = orgUnitManager.getOrgUnit().getParent().getManager();
				if (parentManager != null && !(Objects.equals(parentManager.getManager().getUuid(), person.getUuid()))) {
					return parentManager;
				} else {
					//find different manager all the way up the hierarchy
					OrgUnit parentOrgUnit = orgUnitManager.getOrgUnit().getParent().getParent();
					while (parentOrgUnit != null) {
						parentManager = parentOrgUnit.getManager();
						if (parentManager != null && !(Objects.equals(parentManager.getManager().getUuid(), person.getUuid()))) {
							// success found different manager
							return parentManager;
						} else {
							parentOrgUnit = parentOrgUnit.getParent();
						}
					}
				}
			}
		}
		return null;
	}
	
	public static OrgUnitManager getPrimeManager(Person person) {
		return getManagerDifferentFromPerson(person, person.getPrimeAffiliation());
	}

	public static Affiliation getAffiliation(Person person, String employeeId) {
		Affiliation affiliation = null;

		if (StringUtils.hasLength(employeeId)) {
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


	public List<String> findAllSubstituteUuids(Affiliation affiliation, List<Person> managers, Person person, String employeeId) {
		Set<String> substitutes = new HashSet<>();
		// if no affiliation is specified we fall back to getting the affiliation using person and employeeId
		if (affiliation == null ) {
			affiliation = getAffiliation(person, employeeId);
		}

		// if we have an affiliation, we look up any orgunit substitutes
		if( affiliation != null ) {
			var orgUnit = affiliation.getCalculatedOrgUnit();
			if( orgUnit != null ) {
				substitutes.addAll(substituteOrgUnitAssignmentService.getSofdSubstituteUuids(orgUnit.getUuid()));
			}
		}

		// find all managers' substitutes
		for( Person manager : managers ) {
			for (SubstituteAssignment substituteAssignment : manager.getSubstitutes()) {
				boolean validSubstitute = false;

				if (Objects.equals("GLOBAL", substituteAssignment.getContext().getIdentifier())) {
					validSubstitute = true;
				}
				// SOFD affiliations can be data-constrained, so we need to validate a bit
				else if (Objects.equals("SOFD", substituteAssignment.getContext().getIdentifier())) {
					if (affiliation == null || substituteAssignment.getConstraintMappings().isEmpty()) {
						validSubstitute = true;
					}
					else {
						for (SubstituteAssignmentOrgUnitMapping constraint : substituteAssignment.getConstraintMappings()) {
							if (Objects.equals(affiliation.getCalculatedOrgUnit().getUuid(), constraint.getOrgUnit().getUuid())) {
								validSubstitute = true;
								break;
							}
						}
					}
				}

				if (validSubstitute) {
					substitutes.add(substituteAssignment.getSubstitute().getUuid());
				}
			}
		}


		return substitutes.stream().toList();
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

						if (!emailTemplateService.shouldIncludeOrgUnit(child, affiliation.getCalculatedOrgUnit().getUuid())) {
							log.debug("Not sending email for email template child with id " + child.getId() + " for affiliation with uuid " + (affiliation != null ? affiliation.getUuid() : "<null>") + ". The affiliation OU was filtered out.");
							continue;
						}

						processAffiliation(child, dateFormat, person, affiliation);
					}
				}
			}
		}
	}

	private void processAffiliation(EmailTemplateChild templateChild, DateFormat dateFormat, Person person, Affiliation affiliation) {
		var managerResponse = PersonService.getManagerDifferentFromPerson(affiliation.getPerson(), affiliation);
		if (managerResponse == null) {
			log.warn("ProcessAffiliation - OrgUnit: " + affiliation.getCalculatedOrgUnit().getUuid() + " doesn't have a manager.");
			return;
		}

		String userId = getUserIdForAffiliation(affiliation);

		String message = templateChild.getMessage()
				.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), getName(managerResponse.getManager()))
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
				.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), getName(managerResponse.getManager()))
				.replace(EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER.getPlaceholder(), getName(person))
				.replace(EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER.getPlaceholder(), affiliation.getCalculatedOrgUnit().getName())
				.replace(EmailTemplatePlaceholder.TIMESTAMP_PLACEHOLDER.getPlaceholder(), dateFormat.format(affiliation.getStopDate()))
				.replace(EmailTemplatePlaceholder.VENDOR_PLACEHOLDER.getPlaceholder(), (affiliation.getVendor() != null) ? affiliation.getVendor() : "")
				.replace(EmailTemplatePlaceholder.INTERNAL_REFERENCE_PLACEHOLDER.getPlaceholder(), (affiliation.getInternalReference() != null) ? affiliation.getInternalReference() : "")
				.replace(EmailTemplatePlaceholder.DAYS_BEFORE_EVENT.getPlaceholder(), "" + templateChild.getDaysBeforeEvent())
				.replace(EmailTemplatePlaceholder.POSITION_NAME_PLACEHOLDER.getPlaceholder(), AffiliationService.getPositionName(affiliation))
				.replace(EmailTemplatePlaceholder.EMPLOYEE_NUMBER_PLACEHOLDER.getPlaceholder(), affiliation.getEmployeeId() != null ? affiliation.getEmployeeId() : "")
				.replace(EmailTemplatePlaceholder.ACCOUNT_PLACEHOLDER.getPlaceholder(), userId);

		var logContext = new StringBuilder();
		logContext.append("Skabelon: ").append(templateChild.getTitle());
		logContext.append(", ").append("Medarbejder: ").append(PersonService.getName(person));
		logContext.append(", ").append("Medarbejdernummer: ").append(affiliation.getEmployeeId());
		logContext.append(", ").append("Enhed: ").append(affiliation.getCalculatedOrgUnit().getName());
		logContext.append(", ").append("Konto: ").append(userId);

		// handle manual recipients
		List<String> recipients = emailTemplateChildService.getRecipientsList(templateChild.getRecipients());
		for( var recipient : recipients ) {
			var recipientMessage = message.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), recipient);
			var recipientTitle = title = title.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), recipient);
			emailQueueService.queueEmail(recipient, recipientTitle, recipientMessage, 0, templateChild, logContext.toString());
		}

		// handle the rest of the recipients if not sending to manual only
		if (!templateChild.isOnlyManualRecipients()) {
			List<Person> personRecipients = emailTemplateService.getManagerOrSubstitutes(templateChild, managerResponse.getManager(), affiliation.getCalculatedOrgUnit().getUuid());
			for( Person personRecipient : personRecipients ) {
				var email = PersonService.getEmail(personRecipient);
				if( email != null ) {
					var recipientMessage = message.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), PersonService.getName(personRecipient));
					var recipientTitle = title = title.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), PersonService.getName(personRecipient));
					emailQueueService.queueEmail(personRecipient, recipientTitle, recipientMessage, 0, templateChild, logContext.toString());
				}
			}
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
			newOrders = generateExpireOrders(person, new Date(), null, null, person.getLeave().getReason(), person.getLeave().getReasonText(), person.getLeave().isExpireAccounts(), person.getLeave().isDisableAccountOrders());
		}

		// send mail templates with leave ended
		sendLeaveEndedMails(person);

		// if the disable flag was set during leave, then remove it
		if (person.getLeave().isDisableAccountOrders()) {
			person.setDisableAccountOrdersCreate(false);
			person.setDisableAccountOrdersDelete(false);
			person.setDisableAccountOrdersDisable(false);
		}

		// bye bye leave flag
		person.setLeave(null);

		return newOrders;
	}

	public record ExpireToken(String startDate, String stopDate,String reason, String reasonText, boolean expireAccounts, boolean disableAccountOrders) {}
	public List<AccountOrder> generateExpireOrders(Person person, Date activationDate, Date expireDate, Date stopDate, LeaveReason reason, String reasonText, Boolean expireAccounts, Boolean disableAccountOrders) {
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

		String expireTokenJson = null;
		if (reason != null) {
			ExpireToken expireToken = new ExpireToken(
					expireDate != null ? toLocalDate(expireDate).toString() : null,
					stopDate != null ? toLocalDate(stopDate).toString() : null,
					reason.name(),
					reasonText,
					expireAccounts,
					disableAccountOrders);
            try {
                expireTokenJson = mapper.writeValueAsString(expireToken);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize ExpireToken to JSON", e);
            }

        }

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
			order.setToken(expireTokenJson);

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
				if (supportedUserType == null || supportedUserType.isCanOrder() == false || !supportedUserType.isDeactivateEnabled()) {
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

		List<Person> persons = personDao.findByForceStopTrueOrDisableAccountOrdersCreateTrueOrLeaveNotNull();
		if (persons.isEmpty()) {
			return;
		}

		SecurityUtil.fakeLoginSession();

		for (Person person : persons) {
			List<Affiliation> affiliations = person.getAffiliations();

			if (affiliations.stream().allMatch(aff -> aff.getStopDate() != null && aff.getStopDate().before(date10daysAgo))) {
				person.setForceStop(false);
				person.setDisableAccountOrdersCreate(false);
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
				builder.append(" (" + primeAffiliation.get().getCalculatedOrgUnit().getName() + ")");
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

	public void cleanupOldActiveDirectoryData() {
		SecurityUtil.fakeLoginSession();
		List<Person> persons = self.getActive(p -> {
			p.getUsers().forEach(um -> {
				um.getUser().getUserId();
			});
			
			p.getPhones().forEach(pm -> {
				pm.getPhone().getPhoneNumber();
			});
		});

		List<Person> toSave = new ArrayList<>();

		Set<String> personsWithPhotos = photoService.getPersonsWithPhotos();
		
		for (Person person : persons) {
			if (person.getUsers().stream().noneMatch(um -> SupportedUserTypeService.isActiveDirectory(um.getUser().getUserType()) && um.getUser().isPrime())) {
				if (personsWithPhotos.contains(person.getUuid())) {
					log.info("Deleting photo on " + person.getUuid());

					photoService.delete(person.getUuid());
				}

				if (person.getPhones().removeIf(phoneMapping -> Objects.equals(phoneMapping.getPhone().getMaster(), "ACTIVE_DIRECTORY"))) {
					toSave.add(person);
				}
			}
		}

		if (toSave.size() > 0) {
			log.info("cleanupOldActiveDirectoryData : updating " + toSave.size() + " persons");
			self.saveAllIndividually(toSave);
		}
	}
	
	@Transactional
	public void saveAllIndividually(List<Person> persons) {
		for (Person person : persons) {
			person.setLastChanged();

			personDao.save(person);
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

	public String getRevisionJson(Revision<Integer, Person> revision) {
		var result = "[]";
		if (revision != null) {
			try {
				ObjectMapper mapper = new ObjectMapper();
				mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS); // ensures backwards compatibility with old data
				mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
				mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd"));
				result = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(new HistoryPerson(revision.getEntity()));
			} catch (Exception ex) {
				log.warn("Error occured while parsing Person to JSON string. " + ex.getMessage());
			}
		}
		return result;
	}

	public List<Person> getRandomPeople(int limit) {
		return personDao.getRandomPeople(limit);
	}

	public Person findbyAffiliationId(long affiliationId) {
		return personDao.findByAffiliationsId(affiliationId);
	}

	public Person findByAffiliationUuid(String affiliationUuid) {
		return personDao.findByAffiliationsUuid(affiliationUuid);
	}

	public Person findByAffiliationMasterAndMasterId(String master, String masterId) {
		return personDao.findByAffiliationsMasterAndAffiliationsMasterId(master, masterId);
	}

	private void sendLeaveEndedMails(Person person) {
		EmailTemplate template = emailTemplateService.findByTemplateType(EmailTemplateType.PAUSE_MARKING_ENDED);
		for (EmailTemplateChild child : template.getChildren()) {
			// checks if any of the children are enabled
			if (!child.isEnabled()) {
				continue;
			}

			// checks if use have ad's if adRequired() checked
			if (child.isAdRequired() && person.getActiveADAccounts().isEmpty()) {
				continue;
			}

			String message = child.getMessage();
			String primeADAccount = person.getPrimeADAccount() != null ? person.getPrimeADAccount() : "";
			message = message.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), PersonService.getName(person));
			message = message.replace(EmailTemplatePlaceholder.PRIMARY_ACCOUNT_PLACEHOLDER.getPlaceholder(), primeADAccount);
			message = message.replace(EmailTemplatePlaceholder.ALL_ACCOUNTS_PLACEHOLDER.getPlaceholder(), person.getActiveADAccounts());

			String title = child.getTitle();
			message = message.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), PersonService.getName(person));
			message = message.replace(EmailTemplatePlaceholder.PRIMARY_ACCOUNT_PLACEHOLDER.getPlaceholder(), primeADAccount);
			message = message.replace(EmailTemplatePlaceholder.ALL_ACCOUNTS_PLACEHOLDER.getPlaceholder(), person.getActiveADAccounts());

			String logContext = "Pausemarkering er ophørt for medarbejder " + PersonService.getName(person) + " og en digital post besked udsendes til vedkommende";
			emailQueueService.queueEboks(person, title, message, child.getMinutesDelay(), child, logContext);
		}
	}

}
