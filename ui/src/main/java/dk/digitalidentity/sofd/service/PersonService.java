package dk.digitalidentity.sofd.service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

import dk.digitalidentity.sofd.config.RoleConstants;
import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.PersonDao;
import dk.digitalidentity.sofd.dao.model.AccountOrder;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.EmailTemplate;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import dk.digitalidentity.sofd.dao.model.Notification;
import dk.digitalidentity.sofd.dao.model.OrgUnitManager;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.Phone;
import dk.digitalidentity.sofd.dao.model.Post;
import dk.digitalidentity.sofd.dao.model.RevisionId;
import dk.digitalidentity.sofd.dao.model.SupportedUserType;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.UserChangeEmployeeIdQueue;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderStatus;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderType;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplateType;
import dk.digitalidentity.sofd.dao.model.enums.EndDate;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.NotificationType;
import dk.digitalidentity.sofd.security.SecurityUtil;
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

	public List<Person> findByUserType(String userType) {
		return personDao.findDistinctByUsersUserUserTypeAndDeletedFalse(userType);
	}
	
	public List<Person> findByUserTypeAndUserId(String userType, String userId) {
		return personDao.findByUsersUserUserTypeAndUsersUserUserId(userType, userId);
	}
	
	public Person findByUser(User user) {
		return personDao.findByUsersUser(user);
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
	
	public List<Person> findAllTRs() {
		return personDao.getTRs();
	}

	public List<Person> findAllSRs() {
		return personDao.getSRs();
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

		return person.getFirstname() + " " + person.getSurname();
	}
	
	@Transactional
	public void setPostAndName(Person person, Post post, String firstname, String surname) {
        log.info("Updating name and address on person " + person.getUuid());

		if (!SecurityUtil.isUserLoggedIn() && !SecurityUtil.isClientLoggedIn()) {
			SecurityUtil.fakeLoginSession();
		}

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

		person.setFirstname(firstname);
		person.setSurname(surname);
		
		self.save(person);
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
		personDao.deleteByUuid(entities.stream().map(p -> p.getUuid()).collect(Collectors.toSet()));
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

		if (affiliation != null) {
			if (affiliation.getOrgUnit() != null &&
				affiliation.getOrgUnit().getManager() != null &&
				affiliation.getOrgUnit().getManager().getManager() != null) {
				
				return affiliation.getOrgUnit().getManager().getManager();
			}
		}
		
		return null;
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

				personDao.save(person);
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
							if (excludedOUUuids.contains(affiliation.getOrgUnit().getUuid())) {
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
		OrgUnitManager manager = affiliation.getOrgUnit().getManager();
		if (manager == null) {
			log.warn("ProcessAffiliation - OrgUnit: " + affiliation.getOrgUnit().getUuid() + " doesn't have a manager.");
			return;
		}

		List<String> emailRecipients = emailTemplateService.getManagerOrSubstitutes(templateChild, manager.getManager(), affiliation.getOrgUnit().getUuid());

		if (CollectionUtils.isEmpty(emailRecipients)) {
			log.warn("ProcessAffiliation - no email address found.");
			return;
		}

		String userId = getUserIdForAffiliation(affiliation);

		String message = templateChild.getMessage()
				.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, getName(manager.getManager()))
				.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, getName(person))
				.replace(EmailTemplateService.ORGUNIT_PLACEHOLDER, affiliation.getOrgUnit().getName())
				.replace(EmailTemplateService.TIMESTAMP_PLACEHOLDER, dateFormat.format(affiliation.getStopDate()))
				.replace(EmailTemplateService.VENDOR_PLACEHOLDER, (affiliation.getVendor() != null) ? affiliation.getVendor() : "")
				.replace(EmailTemplateService.INTERNAL_REFERENCE_PLACEHOLDER, (affiliation.getInternalReference() != null) ? affiliation.getInternalReference() : "")
				.replace(EmailTemplateService.DAYS_BEFORE_EVENT, "" + templateChild.getDaysBeforeEvent())
				.replace(EmailTemplateService.POSITION_NAME_PLACEHOLDER, AffiliationService.getPositionName(affiliation))
				.replace(EmailTemplateService.EMPLOYEE_NUMBER_PLACEHOLDER, affiliation.getEmployeeId() != null ? affiliation.getEmployeeId() : "")
				.replace(EmailTemplateService.ACCOUNT_PLACEHOLDER, userId);

		String title = templateChild.getTitle()
				.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, getName(manager.getManager()))
				.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, getName(person))
				.replace(EmailTemplateService.ORGUNIT_PLACEHOLDER, affiliation.getOrgUnit().getName())
				.replace(EmailTemplateService.TIMESTAMP_PLACEHOLDER, dateFormat.format(affiliation.getStopDate()))
				.replace(EmailTemplateService.VENDOR_PLACEHOLDER, (affiliation.getVendor() != null) ? affiliation.getVendor() : "")
				.replace(EmailTemplateService.INTERNAL_REFERENCE_PLACEHOLDER, (affiliation.getInternalReference() != null) ? affiliation.getInternalReference() : "")
				.replace(EmailTemplateService.DAYS_BEFORE_EVENT, "" + templateChild.getDaysBeforeEvent())
				.replace(EmailTemplateService.POSITION_NAME_PLACEHOLDER, AffiliationService.getPositionName(affiliation))
				.replace(EmailTemplateService.EMPLOYEE_NUMBER_PLACEHOLDER, affiliation.getEmployeeId() != null ? affiliation.getEmployeeId() : "")
				.replace(EmailTemplateService.ACCOUNT_PLACEHOLDER, userId);
		
		for (String email : emailRecipients) {
			emailService.sendMessage(email, title, message, null, null, templateChild);
		}
	}

	private String getUserIdForAffiliation(Affiliation affiliation) {
		String userId = "";
		List<User> users = affiliation.getPerson().getUsers().stream().map(u -> u.getUser()).collect(Collectors.toList());
		Optional<User> user = users.stream().filter(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType()) && Objects.equals(u.getEmployeeId(), affiliation.getEmployeeId())).findAny();
		if (user.isPresent()) {
			userId = user.get().getUserId();
		} else {
			Optional<User> primeUser = users.stream().filter(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType()) && u.isPrime()).findAny();
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
				AccountOrder order = accountOrderService.createAccountOrder(person, supportedUserType, null, user.getEmployeeId(), new Date(), EndDate.NO);
				order.setRequestedUserId(user.getUserId());
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

				save(person);
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

			if (person.getAffiliations().stream().anyMatch(a -> Objects.equals(a.getEmployeeId(), employeeId) && Objects.equals(configuration.getModules().getLos().getPrimeAffiliationMaster(), a.getMaster()))) {
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

			save(person);
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
	
	public AutoCompleteResult substituteSearchPerson(String term, String uuid) {
		List<Person> persons = personDao.findTop10ByName(term);
		persons.removeIf(p -> p.getUuid().equals(uuid));

		List<ValueData> suggestions = new ArrayList<>();
		for (Person person : persons) {
			StringBuilder builder = new StringBuilder();
			builder.append(PersonService.getName(person));

			Optional<Affiliation> primeAffiliation = person.getAffiliations().stream().filter(a -> a.isPrime()).findFirst();
			if (primeAffiliation.isPresent()) {
				builder.append(" (" + primeAffiliation.get().getOrgUnit().getName() + ")");
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
		var persons = personDao.findByUsersUserUserTypeAndUsersUserMasterId("ACTIVE_DIRECTORY", masterId);
		for( var person : persons ) {
			var userIdsToBeRemoved = new ArrayList<Long>();
			for( var user : person.getUsers())
			{
				// check if user is an AD user and matches input master id
				if( user.getUser().getMaster().equalsIgnoreCase("ActiveDirectory") && user.getUser().getUserType().equalsIgnoreCase("ACTIVE_DIRECTORY") && user.getUser().getMasterId().equalsIgnoreCase(masterId))
				{
					userIdsToBeRemoved.add(user.getId());
					// also find any existing exchange user based on this ad user
					var exchangeUser = person.getUsers().stream().filter(u -> u.getUser().getMaster().equalsIgnoreCase("ActiveDirectory") && u.getUser().getUserType().equalsIgnoreCase("EXCHANGE") && u.getUser().getMasterId().equalsIgnoreCase(user.getUser().getUserId())).findFirst().orElse(null);
					if( exchangeUser != null)
					{
						userIdsToBeRemoved.add(exchangeUser.getId());
					}
				}
			}
			if( !userIdsToBeRemoved.isEmpty() )
			{
				person.getUsers().removeIf(u -> userIdsToBeRemoved.contains(u.getId()));
				self.save(person);
			}
		}
	}
}
