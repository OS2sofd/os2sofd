package dk.digitalidentity.sofd.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.AccountOrderDao;
import dk.digitalidentity.sofd.dao.OrgUnitAccountOrderDao;
import dk.digitalidentity.sofd.dao.model.AccountOrder;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.EmailQueue;
import dk.digitalidentity.sofd.dao.model.EmailTemplate;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import dk.digitalidentity.sofd.dao.model.Notification;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.OrgUnitAccountOrder;
import dk.digitalidentity.sofd.dao.model.OrgUnitAccountOrderType;
import dk.digitalidentity.sofd.dao.model.OrgUnitAccountOrderTypePosition;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SupportedUserType;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderRule;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderStatus;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderType;
import dk.digitalidentity.sofd.dao.model.enums.AffiliationType;
import dk.digitalidentity.sofd.dao.model.enums.CustomerSetting;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplateType;
import dk.digitalidentity.sofd.dao.model.enums.EndDate;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.NotificationType;
import dk.digitalidentity.sofd.service.model.UserAudRow;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@EnableCaching
@EnableScheduling
public class AccountOrderService {

	/*
	 * SELECT u.disabled, r.timestamp
	 * FROM users_aud u
  	 *		JOIN revisions r ON r.id = u.rev
  	 *		WHERE u.user_id = ?
     *			AND u.user_type = 'ACTIVE_DIRECTORY'
     *			AND u.disabled IS NOT NULL
     *			AND r.timestamp > ?;
	 */
	private static final String SELECT_AUD_USERS = "SELECT u.disabled, r.timestamp FROM users_aud u JOIN revisions r ON r.id = u.rev WHERE u.user_id = ? AND u.user_type = 'ACTIVE_DIRECTORY' AND u.disabled IS NOT NULL AND r.timestamp > ?;";

	@Autowired
	private AccountOrderDao accountOrderDao;
	
	@Autowired
	private PersonService personService;

	@Autowired
	private OrgUnitAccountOrderDao orgUnitAccountOrderDao;
		
	@Autowired
	private SupportedUserTypeService supportedUserTypeService;
	
	@Autowired
	private OrgUnitService orgUnitService;
	
	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private AffiliationService affiliationService;
	
	@Autowired
	private NotificationService notificationService;

	@Autowired
	private AccountOrderService self;
	
	@Autowired
	private UsernameGeneratorService usernameGeneratorService;
	
	@Autowired
	private EmailTemplateService emailTemplateService;

	@Autowired
	private EmailQueueService emailQueueService;
	
	@Autowired
	private UserService userService;
	
	@Qualifier("defaultTemplate")
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private SettingService settingService;

	public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
	    Set<Object> seen = ConcurrentHashMap.newKeySet();
	    return t -> seen.add(keyExtractor.apply(t));
	}

	// has to be public to be @Cacheable
	@Cacheable(value = "internalCacheAll")
	public Map<String, OrgUnitAccountOrder> internalCachedOrgUnitAccountOrderMap() {
		return orgUnitAccountOrderDao.findAll().stream().collect(Collectors.toMap(OrgUnitAccountOrder::getOrgunitUuid, Function.identity()));
	}
	
	// create and delete in one go, transaction needed
	@Transactional(rollbackFor = Exception.class)
	public void save(List<AccountOrder> accounts) {
		if (accounts.size() > 0) {
			Set<String> personUuids = accounts.stream().map(a -> a.getPersonUuid()).collect(Collectors.toSet());
			
			// bit of a dirty hack to look at the first entry to decide the type for all of them
			// but we always call with a set of the same type
			List<AccountOrderType> types = new ArrayList<>();
			switch (accounts.get(0).getOrderType()) {
				case EXPIRE:
					break;
				case CREATE:
					types.add(AccountOrderType.DEACTIVATE);
					types.add(AccountOrderType.DELETE);
					break;
				case DEACTIVATE:
				case DELETE:
					types.add(AccountOrderType.CREATE);
					break;
			}

			// delete old ones (of the same type)
			accountOrderDao.deleteByStatusAndPersonUuidInAndOrderTypeIn(AccountOrderStatus.PENDING, personUuids, types);

			// create new ones
			for (AccountOrder account : accounts) {
				save(account);
			}
		}
	}

	public void delete(AccountOrder order) {
		accountOrderDao.delete(order);
	}

	public List<AccountOrder> getPendingOrders(Person person) {
		return accountOrderDao.findByPersonUuidAndStatus(person.getUuid(), AccountOrderStatus.PENDING);
	}

	public AccountOrder save(AccountOrder order) {
		// on new create orders, generate a userId
		if (!StringUtils.hasLength(order.getRequestedUserId())) {
			if (order.getId() == 0 && order.getOrderType().equals(AccountOrderType.CREATE)) {
				Person person = personService.getByUuid(order.getPersonUuid());
				SupportedUserType userType = supportedUserTypeService.findByKey(order.getUserType());
	
				if (userType == null) {
					log.error("Attemping to save account order with an unknown userType: " + order.getUserType());
					
					return order;
				}
				else {
					String userId = usernameGeneratorService.getUsername(person, order.getEmployeeId(), userType.getKey(), order.getLinkedUserId());
					if (userId != null) {
						order.setRequestedUserId(userId);
					}
					else {
						log.warn("Failed to generate a username for accountOrder: " + userType.getName() + " / " + PersonService.getName(person) + " / " + person.getUuid());
	
						// flag order as failed
						order.setStatus(AccountOrderStatus.FAILED);
						order.setMessage("Det var ikke muligt at danne et brugernavn for denne bruger");
						
						// notify about failure
						Notification notification = new Notification();
						notification.setActive(true);
						notification.setAffectedEntityName(PersonService.getName(person));
						notification.setAffectedEntityType(EntityType.PERSON);
						notification.setAffectedEntityUuid(person.getUuid());
						notification.setMessage(userType.getName() + ": Navnestandarden kunne ikke anvendes for denne person");
						notification.setNotificationType(NotificationType.ACCOUNT_ORDER_FAILURE);
						notification.setCreated(new Date());
						notificationService.save(notification);
					}
				}
			}
		}

		return accountOrderDao.save(order);
	}

	public List<AccountOrder> getPendingCreateOrdersForPerson(String uuid) {
		return accountOrderDao.findByStatusAndOrderTypeAndPersonUuid(AccountOrderStatus.PENDING, AccountOrderType.CREATE, uuid);
	}
	
	public List<AccountOrder> getPendingAndBlockedCreateOrdersForPerson(String uuid) {
		List<AccountOrder> result = accountOrderDao.findByStatusAndOrderTypeAndPersonUuid(AccountOrderStatus.PENDING, AccountOrderType.CREATE, uuid);
		result.addAll(accountOrderDao.findByStatusAndOrderTypeAndPersonUuid(AccountOrderStatus.BLOCKED, AccountOrderType.CREATE, uuid));

		return result;
	}

	public List<AccountOrder> getOrdersByUserType(String userType) {
		return accountOrderDao.findByUserType(userType);
	}

	/**
	 * Get all pending orders (that are ready to be processed) of a specific type
	 */
	public List<AccountOrder> getPendingOrders(String userType, AccountOrderType orderType) {
		return accountOrderDao.findByStatusAndUserTypeAndOrderTypeAndActivationTimestampBefore(AccountOrderStatus.PENDING, userType, orderType, new Date());
	}

	public List<AccountOrder> findAll() {
		return accountOrderDao.findAll();
	}

	private List<AccountOrder> findAllCreateOrders() {
		return accountOrderDao.findByOrderType(AccountOrderType.CREATE);
	}
	
	public List<AccountOrder> findAllCompletedOpusCreateOrders(Person person) {
		return accountOrderDao.findByPersonUuidAndOrderTypeAndStatusAndUserType(person.getUuid(), AccountOrderType.CREATE, AccountOrderStatus.CREATED, SupportedUserTypeService.getOpusUserType());
	}

	private List<AccountOrder> findAllDeleteAndDeactivateOrders() {
		return accountOrderDao.findByOrderTypeIn(AccountOrderType.DEACTIVATE, AccountOrderType.DELETE);
	}

	public AccountOrder findById(long id) {
		return accountOrderDao.findById(id);
	}

	public List<AccountOrder> findPendingDeactivation(Person person) {
		return accountOrderDao.findByStatusAndOrderTypeAndPersonUuid(AccountOrderStatus.PENDING, AccountOrderType.DEACTIVATE, person.getUuid());
	}

	@Transactional
	public void cleanupOld() {
		List<AccountOrder> orders = accountOrderDao.findAll();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -14);
        Date deleteSuccessful = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, -14);
        Date deleteFailed = cal.getTime();

		for (AccountOrder order : orders) {
			switch (order.getStatus()) {
				case EXPIRED:
				case CREATED:
				case DEACTIVATED:
				case DELETED:
				case FAILED:
				case REACTIVATED:
					if (order.getModifiedTimestamp().before(deleteSuccessful)) {
						accountOrderDao.delete(order);
					}
					break;
				case PENDING:
					if (order.getActivationTimestamp().before(deleteFailed)) {
						log.warn("Pending order deleted due to not beeing processed: " + order.getId() + " / " + order.getUserType() + " / " + order.getPersonUuid());
						accountOrderDao.delete(order);
					}
					break;
				case BLOCKED:
					// faktisk ikke nødvendigt, de bliver slettet via cascade SQL regel når den linkede konto bliver slettet
					break;
			}
		}
	}

	public AccountOrder deactivateOrDeleteAccountOrder(AccountOrderType type, Person person, String employeeId, String userType, String userIdToDeactivateOrDelete) {
		return deactivateOrDeleteAccountOrder(type, person, employeeId, userType, userIdToDeactivateOrDelete, new Date(), null);
	}
	
	public AccountOrder deactivateOrDeleteAccountOrder(AccountOrderType type, Person person, String employeeId, String userType, String userIdToDeactivateOrDelete, Date activationTime) {
		return deactivateOrDeleteAccountOrder(type, person, employeeId, userType, userIdToDeactivateOrDelete, activationTime, null);
	}
	
	public AccountOrder deactivateOrDeleteAccountOrder(AccountOrderType type, Person person, String employeeId, String userType, String userIdToDeactivateOrDelete, Date activationTime, String apiUserId) {
		if (!type.equals(AccountOrderType.DEACTIVATE) && !type.equals(AccountOrderType.DELETE)) {
			throw new RuntimeException("Invalid ordertype: " + type);
		}

		AccountOrder accountOrder = new AccountOrder();
		accountOrder.setOrderType(type);
		accountOrder.setStatus(AccountOrderStatus.PENDING);
		accountOrder.setActivationTimestamp(activationTime);
		accountOrder.setOrderedTimestamp(new Date());
		accountOrder.setPersonUuid(person.getUuid());
		accountOrder.setUserType(userType);		
		accountOrder.setRequestedUserId(userIdToDeactivateOrDelete);

		if (SupportedUserTypeService.isOpus(userType) || !supportedUserTypeService.findByKey(userType).isSingleUserMode()) {
			accountOrder.setEmployeeId(employeeId);
		}

		if (StringUtils.hasLength(apiUserId)) {
			accountOrder.setRequesterApiUserId(apiUserId);
		}
		else {
			Person requesterPerson = personService.getLoggedInPerson();
			if (requesterPerson != null) {
				accountOrder.setRequesterUuid(requesterPerson.getUuid());
			}
		}

		return accountOrder;
	}

	// TODO: burde tage en Affiliation (som kan være null) i stedet for et employeeId... det afhænger meget af det faktiske flow hvad
	//       der skal ske tænker jeg. Så i databasen bør vi også gemme et affiliationUuid i stedet for et employeeId
	public AccountOrder createAccountOrder(Person person, SupportedUserType userType, String linkedUserId, String employeeId, Date activationDate, EndDate endDateValue) {
		return createAccountOrder(person, userType, linkedUserId, employeeId, activationDate, endDateValue, null, false);
	}

	private AccountOrder createAccountOrder(Person person, SupportedUserType userType, String linkedUserId, String employeeId, boolean doNotLogRequester) {
		return createAccountOrder(person, userType, linkedUserId, employeeId, new Date(), EndDate.NO, null, doNotLogRequester);
	}

	private AccountOrder createAccountOrder(Person person, SupportedUserType userType, String linkedUserId, String employeeId, Date activationDate, EndDate endDateValue, String apiUserId, boolean doNotLogRequester) {
		activationDate = (activationDate != null) ? activationDate : new Date();

		// if the userType has a delay configured, add that to the activationDate
		if (userType.getMinutesDelay() > 0) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(activationDate);
			cal.add(Calendar.MINUTE, (int) userType.getMinutesDelay());

			activationDate = cal.getTime();
		}

		Calendar cal = Calendar.getInstance();
		cal.setTime(activationDate);
		cal.set(Calendar.HOUR_OF_DAY, 20);
		cal.set(Calendar.MINUTE, 0);

		Date endDate = null;
		switch (endDateValue) {
			case TOMORROW:
				cal.add(Calendar.DATE, 1);
				endDate = cal.getTime();
				break;
			case DAYS3:
				cal.add(Calendar.DATE, 3);
				endDate = cal.getTime();
				break;
			case WEEK:
				cal.add(Calendar.DATE, 7);
				endDate = cal.getTime();
				break;
			case MONTH:
				cal.add(Calendar.DATE, 30);
				endDate = cal.getTime();
				break;
			case NO:
				break;
		}

		AccountOrder accountOrder = new AccountOrder();
		accountOrder.setOrderType(AccountOrderType.CREATE);
		accountOrder.setStatus(AccountOrderStatus.PENDING);
		accountOrder.setActivationTimestamp(activationDate);
		accountOrder.setEndDate(endDate);
		accountOrder.setOrderedTimestamp(new Date());
		accountOrder.setPersonUuid(person.getUuid());
		accountOrder.setUserType(userType.getKey());
		accountOrder.setLinkedUserId(linkedUserId);
		
		if (SupportedUserTypeService.isOpus(userType.getKey()) || !userType.isSingleUserMode()) {
			accountOrder.setEmployeeId(employeeId);
		}

		if (StringUtils.hasLength(apiUserId)) {
			accountOrder.setRequesterApiUserId(apiUserId);
		}
		else if (!doNotLogRequester) {
			Person requesterPerson = personService.getLoggedInPerson();
			if (requesterPerson != null) {
				accountOrder.setRequesterUuid(requesterPerson.getUuid());
			}
		}

		return accountOrder;
	}
	
	// create and delete in one go, transaction needed
	@Transactional(rollbackFor = Exception.class)
	public OrgUnitAccountOrder setAccountOrderSettings(OrgUnit orgUnit, OrgUnitAccountOrder accountOrders) {
		OrgUnitAccountOrder originalOrder = getAccountOrderSettings(orgUnit, true);
		boolean changes = false;
		
		for (OrgUnitAccountOrderType originalType : originalOrder.getTypes()) {
			for (OrgUnitAccountOrderType type : accountOrders.getTypes()) {
				if (originalType.getUserType().equals(type.getUserType())) {
					AccountOrderRule oldRule = originalType.getRule();

					if (!type.getRule().equals(originalType.getRule())) {
						originalType.setRule(type.getRule());
						changes = true;
					}
					
					if (originalType.getRule().equals(AccountOrderRule.BY_POSITION_NAME)) {
						for (OrgUnitAccountOrderTypePosition originalPositionRule : originalType.getPositions()) {
							for (OrgUnitAccountOrderTypePosition positionRule : type.getPositions()) {
								if (originalPositionRule.getPositionName().equals(positionRule.getPositionName())) {
									if (!originalPositionRule.getRule().equals(positionRule.getRule())) {
										originalPositionRule.setRule(positionRule.getRule());
										changes = true;
									}
									
									break;
								}
							}
						}
					}
					else if (oldRule.equals(AccountOrderRule.BY_POSITION_NAME)) {
						originalType.setPositions(new ArrayList<>());
						changes = true;
					}
					
					break;
				}
			}
		}
		
		if (changes) {
			originalOrder = orgUnitAccountOrderDao.save(originalOrder);
			
			// trigger ordering new accounts if needed
			List<AccountOrder> orderAccounts = getAccountsToCreate(orgUnit.getAffiliations(), true, originalOrder, false);
			if (orderAccounts.size() > 0) {
				save(orderAccounts);
			}
		}
		
		return originalOrder;
	}

	public OrgUnitAccountOrder getAccountOrderSettings(OrgUnit orgUnit, boolean bypassCache) {
		OrgUnitAccountOrder result = accountOrderTemplate(orgUnit);

		// Overwrite template with information from database.
		// This ensures that only active/valid information is retrieved, even if
		// the database contains old/garbage data
		OrgUnitAccountOrder order;
		if (bypassCache) {
			order = orgUnitAccountOrderDao.findByOrgunitUuid(orgUnit.getUuid());
		}
		else {
			order = self.internalCachedOrgUnitAccountOrderMap().get(orgUnit.getUuid());
		}

		if (order != null) {
			result.setId(order.getId());

			for (OrgUnitAccountOrderType type : order.getTypes()) {
				for (OrgUnitAccountOrderType templateType : result.getTypes()) {
					if (templateType.getUserType().equals(type.getUserType())) {
						templateType.setId(type.getId());
						templateType.setRule(type.getRule());

						if (type.getRule().equals(AccountOrderRule.BY_POSITION_NAME)) {
							for (OrgUnitAccountOrderTypePosition templatePosition : templateType.getPositions()) {
								for (OrgUnitAccountOrderTypePosition position : type.getPositions()) {
									if (templatePosition.getPositionName().equals(position.getPositionName())) {
										templatePosition.setRule(position.getRule());
										break;
									}
								}
							}
						}

						break;
					}
				}
			}
		}
		
		return result;
	}
	
	// run every 30 minutes
	@Scheduled(fixedRate = 1000 * 60 * 30)
	public void cacheClearTask() {
		self.cacheClear();
	}

	@CacheEvict(value = "internalCacheAll", allEntries = true)
	public void cacheClear() {
		; // do nothing, annotation handles actual logic
	}
	
	private OrgUnitAccountOrder accountOrderTemplate(OrgUnit orgUnit) {
		OrgUnitAccountOrder order = new OrgUnitAccountOrder();
		order.setOrgunitUuid(orgUnit.getUuid());
		order.setTypes(new ArrayList<>());
		
		Set<String> positionNames = orgUnitService.getPositionNames(orgUnit, false, false);
		
		for (SupportedUserType userType : supportedUserTypeService.findAll()) {
			if (!userType.isCanOrder()) {
				continue;
			}

			OrgUnitAccountOrderType orderType = new OrgUnitAccountOrderType();
			orderType.setOrgUnitAccountOrder(order);
			orderType.setRule(AccountOrderRule.UNDECIDED);
			orderType.setUserType(userType.getKey());
			
			orderType.setPositions(new ArrayList<>());
			for (String positionName : positionNames) {
				OrgUnitAccountOrderTypePosition orderTypePosition = new OrgUnitAccountOrderTypePosition();
				orderTypePosition.setOrgUnitAccountOrderType(orderType);
				orderTypePosition.setPositionName(positionName);
				orderTypePosition.setRule(AccountOrderRule.UNDECIDED);
				
				orderType.getPositions().add(orderTypePosition);
			}
			
			order.getTypes().add(orderType);
		}
		
		return order;
	}
	
	/**
	 * Used by our STOP marker code for immediate action
	 */
	public List<AccountOrder> getAccountsToDeleteImmediately(Person person) {
		return getAccountsToDelete(Collections.singletonList(person), false);
	}

	private List<AccountOrder> getAccountsToDelete(List<Person> persons, boolean respectDeleteDays) {
		List<AccountOrder> accountDeletesResult = new ArrayList<>();

		// read settings
		List<String> masters = configuration.getScheduled().getAccountOrderGeneration().getMasters();
		
		// find orderable usertypes
		List<SupportedUserType> orderableUserTypesAsObjects = supportedUserTypeService.findAll().stream()
				.filter(u -> u.isCanOrder())
				.collect(Collectors.toList());

		Map<String, OffsetDays> offsetDays = new HashMap<>();
		for (SupportedUserType userType : orderableUserTypesAsObjects) {
			OffsetDays value = getOffsetDays(userType);
			if (value != null) {
				offsetDays.put(userType.getKey(), value);
			}
		}

		List<String> orderableUserTypes = orderableUserTypesAsObjects.stream().map(u -> u.getKey()).collect(Collectors.toList());
		
		for (Person person : persons) {
			// note we also grab deactivated users, as we might need to create DELETE orders for those
			List<User> users = PersonService.getUsers(person).stream().filter(u -> orderableUserTypes.contains(u.getUserType())).collect(Collectors.toList());
			
			// ignore persons with no relevant user accounts
			if (users.size() == 0) {
				continue;
			}

			// find active affiliations of types that can affect account orders
			List<Affiliation> affiliations = person.getAffiliations().stream()
					.filter(a -> masters.contains(a.getMaster()))
					.collect(Collectors.toList());
			
			// if the person has no affiliations controlled by masters that are used in
			// the IdM process (or the person has no affiliations at all), we skip deactivation
			if (affiliations.size() == 0) {
				continue;
			}

			for (User user : users) {
				if (respectDeleteDays && !offsetDays.containsKey(user.getUserType())) {
					continue;
				}

				SupportedUserType supportedUserType = supportedUserTypeService.findByKey(user.getUserType());
				if (supportedUserType == null) {
					log.warn("Unknown userType when iterating over persons user accounts: " + user.getUserType());
					continue;
				}

				affiliations = AffiliationService.notStoppedAffiliations(affiliations);

				// if there are no active affiliations, we need to order delete/deactivate's
				boolean delete = (affiliations.size() == 0);

				// but even if we are not in the delete scenario, we might still be in the !singleAccount scenario,
				// where accounts are matched to affiliations, so we need to check if the matched affiliation has
				// expired, causing the user account be to flagged for deactivate/delete
				if (!delete && !supportedUserType.isSingleUserMode()) {
					boolean foundMatchingAffiliation = false;

					for (Affiliation affiliation : affiliations) {
						if (user.getEmployeeId() != null && user.getEmployeeId().equals(affiliation.getEmployeeId())) {
							foundMatchingAffiliation = true;
							break;
						}
					}
					
					delete = !(foundMatchingAffiliation);
				}

				if (delete) {
					Date deactivateDate = new Date();
					Date deleteDate = null;

					if (respectDeleteDays) {
						deactivateDate = offsetDays.get(user.getUserType()).deactivateDate;
						deleteDate = offsetDays.get(user.getUserType()).deleteDate;
					}
					else {
						// we only deactivate immediately, the delete will happen as scheduled in the future
						if (offsetDays.containsKey(user.getUserType())) {
							deleteDate = offsetDays.get(user.getUserType()).deleteDate;
						}
					}
					
					// for EXCHANGE we need to know the linked AD account
					String linkedUserId = null;
					if (SupportedUserTypeService.isExchange(user.getUserType())) {
						linkedUserId = user.getMasterId();
					}

					// no reason to deactivate already deactivated users ;)
					if (user.isDisabled() == false && deactivateDate != null) {
						AccountOrder accountOrder = deactivateOrDeleteAccountOrder(AccountOrderType.DEACTIVATE, person, user.getEmployeeId(), user.getUserType(), user.getUserId(), deactivateDate);
						accountOrder.setLinkedUserId(linkedUserId);
						accountDeletesResult.add(accountOrder);
					}
					
					if (deleteDate != null) {
						AccountOrder accountOrder = deactivateOrDeleteAccountOrder(AccountOrderType.DELETE, person, user.getEmployeeId(), user.getUserType(), user.getUserId(), deleteDate);
						accountOrder.setLinkedUserId(linkedUserId);
						accountDeletesResult.add(accountOrder);
					}
				}
			}
		}
		
		return accountDeletesResult;
	}

	class OffsetDays {
		long daysBeforeToCreate;
		Date deactivateDate; 
		Date deleteDate;
	}

	private OffsetDays getOffsetDays(SupportedUserType supportedUserType) {
		// read initial settings
		long daysToDeactivate = supportedUserType.getDaysToDeactivate();
		long daysToDelete = supportedUserType.getDaysToDelete();

		// if these values are set to 0, it disables the actions
		if (daysToDeactivate <= 0 && daysToDelete <= 0) {
			return null;
		}

		OffsetDays offsetDays = new OffsetDays();
		offsetDays.daysBeforeToCreate = supportedUserType.getDaysBeforeToCreate();

		// ordinary bulk actions are dealt with at 9:00 ;)
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 9);
		cal.set(Calendar.MINUTE, 0);
		Date todayAtNine = cal.getTime();

		if (daysToDeactivate > 0) {
			cal.add(Calendar.DATE, (int) daysToDeactivate);
			offsetDays.deactivateDate = cal.getTime();
		}
		
		if (daysToDelete > 0) {
			cal.setTime(todayAtNine);
			cal.add(Calendar.DATE, (int) daysToDelete);
			offsetDays.deleteDate = cal.getTime();
		}

		return offsetDays;
	}

	/**
	 * Returns a Set of userIds matching AD accounts that are available for having an EXCHANGE account associated with
	 * them... any account that already has an EXCHANGE account associated is not returned from this method
	 */
	public Set<String> getActiveDirectoryUsersForExchangeAccount(List<Affiliation> affiliations, boolean onlyPrime) {
		Set<String> userIds = new HashSet<>();

		boolean singleUserMode = supportedUserTypeService.findByKey(SupportedUserTypeService.getActiveDirectoryUserType()).isSingleUserMode();
		
		for (Affiliation affiliation : affiliations) {
			for (User user : affiliation.getPerson().onlyActiveUsers()) {
				if (!user.getUserType().equals(SupportedUserTypeService.getActiveDirectoryUserType())) {
					continue;
				}

				if (onlyPrime && !user.isPrime()) {
					continue;
				}

				// check if this AD account is already associated with an existing Exchange account
				boolean hasEmail = affiliation.getPerson().onlyActiveUsers().stream().anyMatch(u ->
							SupportedUserTypeService.isExchange(u.getUserType()) &&
							u.getMasterId().equals(user.getUserId()));

				if (hasEmail) {
					continue;
				}
	
				// if we are not running in single-account mode, then we need to match
				// employeeId values on user and affiliation objects
				if (!singleUserMode &&
					!Objects.equals(affiliation.getEmployeeId(), user.getEmployeeId())) {
	
					continue;
				}
				
				userIds.add(user.getUserId());
			}
		}

		return userIds;
	}
	
	/**
	 * should only be used for dry-runs, when trying out alternative rules for ordering
	 */
	public List<AccountOrder> getAccountsToCreate(OrgUnit orgUnit, OrgUnitAccountOrder rules) {
		// as this is used by the GUI when trying out changes to orders, we need to take existing orders into
		// account before informing the user of the changes that will happen
		return getAccountsToCreate(orgUnit.getAffiliations(), true, rules, false);
	}

	/**
	 * primary method - called by our save intercepter and our scheduled task. This is what
	 * generates the account orders based on all the active rules
	 */
	public List<AccountOrder> getAccountsToCreate(List<Affiliation> affiliations, boolean takeExistingOrdersIntoAccount, boolean doNotLogRequester) {
		return getAccountsToCreate(affiliations, takeExistingOrdersIntoAccount, null, doNotLogRequester);
	}

	private List<AccountOrder> getAccountsToCreate(List<Affiliation> affiliations, boolean takeExistingOrdersIntoConsideration, OrgUnitAccountOrder rules, boolean doNotLogRequester) {
		List<AccountOrder> accountOrdersResult = new ArrayList<>();

		if (!configuration.getModules().getAccountCreation().isEnabled()) {
			return accountOrdersResult;
		}
		
		// filter out affiliations that are not of the supported type
		List<String> masters = configuration.getScheduled().getAccountOrderGeneration().getMasters();
		affiliations = affiliations.stream()
								   .filter(a -> masters.contains(a.getMaster()) && a.getPerson().isDisableAccountOrders() == false)
								   .collect(Collectors.toList());
		
		if (affiliations.size() == 0) {
			return accountOrdersResult;
		}

		for (SupportedUserType userType : supportedUserTypeService.findAll()) {
			if (!userType.isCanOrder()) {
				continue;
			}
			
			List<Affiliation> relevantAffiliations = new ArrayList<>(affiliations);
			if (SupportedUserTypeService.isOpus(userType.getKey())) {
				// when ordering OPUS accounts, only OPUS owned affiliations are relevant
				relevantAffiliations = relevantAffiliations.stream()
						.filter(a -> "OPUS".equals(a.getMaster()))
						.collect(Collectors.toList());
			}
			
			int offsetDays = (int) userType.getDaysBeforeToCreate();
			boolean isExchange = SupportedUserTypeService.isExchange(userType.getKey());
			
			for (Affiliation affiliation : relevantAffiliations) {

				// wait until prerequisites are in place
				if (!hasRequiredOtherAccount(userType, affiliation)) {
					continue;
				}

				// actual logic happens here
				if (!shouldOrderAccountOfType(userType.getKey(), affiliation, offsetDays, false, rules)) {
					continue;
				}

				// exchange accounts must be linked to an AD account
				String linkedUserId = null;
				if (isExchange) {
					linkedUserId = getLinkedUserId(userType, affiliation);

					if (linkedUserId == null) {
						continue;
					}
				}
				
				// for now, we assume we need to order a new account (taking existing orders into consideration can change this)
				boolean generateOrder = true;

				// is there an existing CREATE order for this type of user account, then do not create a new one
				String employeeId = affiliation.getEmployeeId();

				if (takeExistingOrdersIntoConsideration) {
					List<AccountOrder> existingOrders = accountOrderDao.findByOrderTypeAndPersonUuid(AccountOrderType.CREATE, affiliation.getPerson().getUuid());
					
					if (!userType.isSingleUserMode()) {
						generateOrder = existingOrders.stream()
								.noneMatch(o -> o.getUserType().equals(userType.getKey()) && Objects.equals(employeeId, o.getEmployeeId()));
					}
					else {
						generateOrder = existingOrders.stream()
								.noneMatch(o -> o.getUserType().equals(userType.getKey()));
						
						// when orders are not associated with employeeIds, we need to check if there are other orders generated
						// by previous iterations of this method
						if (generateOrder) {
							generateOrder = accountOrdersResult.stream()
												.noneMatch(o -> o.getPersonUuid().contentEquals(affiliation.getPerson().getUuid()) &&
														   o.getUserType().contentEquals(userType.getKey()));
						}
					}
				}

				if (generateOrder) {
					AccountOrder accountOrder = createAccountOrder(affiliation.getPerson(), userType, linkedUserId, employeeId, doNotLogRequester);
					accountOrdersResult.add(accountOrder);
				}
			}
		}
		
		return accountOrdersResult;
	}

	private boolean hasRequiredOtherAccount(SupportedUserType userType, Affiliation affiliation) {
		// if the userType in question depends on another userType, the
		// person associated with the affiliation MUST have such a user account,
		// otherwise no order can be created
		if (userType.getDependsOn() != null) {
			boolean hasRequiredUserType = false;

			for (User existingUserAccount : affiliation.getPerson().onlyActiveUsers()) {
				if (existingUserAccount.getUserType().equals(userType.getDependsOn().getKey())) {
					hasRequiredUserType = true;
					break;
				}
			}

			if (!hasRequiredUserType) {
				return false;
			}
		}
		
		return true;
	}

	private String getLinkedUserId(SupportedUserType userType, Affiliation affiliation) {
		String linkedUserId = null;

		// besides the usual checks, we also need to ensure that the person has at least
		// one AD account, which does not already have an email address associated
		
		Set<String> potentialUserIds = getActiveDirectoryUsersForExchangeAccount(Collections.singletonList(affiliation), true);
		if (potentialUserIds.size() == 0) {
			log.warn("Could not find any available AD accounts to link Exchange account to");
		}
		else {

			// the associated AD account to map this EXCHANGE account to
			if (userType.isSingleUserMode()) {
				linkedUserId = potentialUserIds.iterator().next();
			}
			else {
				List<User> users = affiliation.getPerson().onlyActiveUsers();
				
				for (String adUserId : potentialUserIds) {
					Optional<User> oUser = users.stream().filter(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType()) &&
																	  Objects.equals(u.getEmployeeId(), affiliation.getEmployeeId()))
								  						 .findFirst();
					
					if (oUser.isPresent()) {
						linkedUserId = adUserId;
						break;
					}
				}
				
				if (linkedUserId == null) {
					log.warn("Could not find any available AD accounts to link Exchange account to (employeeId match-case)");
				}
			}
		}

		return linkedUserId;
	}

	public boolean shouldOrderAccountOfType(String userType, Affiliation affiliation, int offsetDays, boolean ignoreExistingAccounts) {
		return shouldOrderAccountOfType(userType, affiliation, offsetDays, ignoreExistingAccounts, null);
	}
	
	private boolean shouldOrderAccountOfType(String userType, Affiliation affiliation, int offsetDays, boolean ignoreExistingAccounts, OrgUnitAccountOrder rules) {
		if (affiliation == null) {
			return false;
		}
		
		return shouldOrderAccountOfType(userType, Collections.singletonList(affiliation), offsetDays, ignoreExistingAccounts, rules);
	}

	// alternativeRules should ONLY ever be filled out when calling with affiliations from the same OrgUnit!
	private boolean shouldOrderAccountOfType(String userType, List<Affiliation> affiliations, int offsetDays, boolean ignoreExistingAccounts, OrgUnitAccountOrder alternativeRules) {

		// filter inactive affiliations, as those should NEVER trigger account orders
		affiliations = AffiliationService.onlyActiveAffiliations(affiliations, offsetDays);

		if (affiliations == null || affiliations.size() == 0) {
			return false;
		}

		SupportedUserType supportedUserType = supportedUserTypeService.findByKey(userType);
		if (supportedUserType == null) {
			log.warn("Unknown userType in shouldOrderAccountOfType: " + userType);
			return false;
		}
		
		// singleUserMode check goes above the individual affiliation (but check for non-singleUserMode inside loop below
		if (!ignoreExistingAccounts && supportedUserType.isSingleUserMode()) {
			Person person = affiliations.get(0).getPerson();
			
			for (User user : person.onlyActiveUsers()) {
				if (user.getUserType().equals(userType)) {
					return false;
				}
			}
		}

		for (Affiliation affiliation : affiliations) {

			// external affiliations that does not inherit privileges are skipped
			if (affiliation.getAffiliationType().equals(AffiliationType.EXTERNAL) && !affiliation.isInheritPrivileges()) {
				continue;
			}

			// for non-singleUserMode we need to check if an account already exists for this affiliation
			if (!ignoreExistingAccounts && !supportedUserType.isSingleUserMode()) {
				Person person = affiliations.get(0).getPerson();
				boolean doNotOrder = false;

				for (User user : person.onlyActiveUsers()) {
					if (user.getUserType().equals(userType) && Objects.equals(user.getEmployeeId(), affiliation.getEmployeeId())) {
						doNotOrder = true;
						break;
					}
				}
				
				if (doNotOrder) {
					continue;
				}
			}

			OrgUnitAccountOrder rules = null;
			if (alternativeRules != null) {
				rules = alternativeRules;
			}
			else {
				rules = self.getAccountOrderSettings(affiliation.getOrgUnit(), false);
			}

			for (OrgUnitAccountOrderType orderType : rules.getTypes()) {
				if (!orderType.getUserType().equals(userType)) {
					continue;
				}
				
				switch (orderType.getRule()) {
					case UNDECIDED:
					case DISABLED:
						break;
					case EVERYONE:
						return true;
					case EVERYONE_EXCEPT_HOURLY_PAID:
						if (StringUtils.hasLength(configuration.getModules().getAccountCreation().getHourlyWageCode()) &&
							 configuration.getModules().getAccountCreation().getHourlyWageCode().equals(affiliation.getEmploymentTerms())) {
							break;
						}

						return true;
					case BY_POSITION_NAME:
						for (OrgUnitAccountOrderTypePosition positionRule : orderType.getPositions()) {
							if (affiliation.getPositionName().equals(positionRule.getPositionName())) {
								switch (positionRule.getRule()) {
									case BY_POSITION_NAME: // not used
									case DISABLED:
									case UNDECIDED:
										break;
									case EVERYONE:
										return true;
									case EVERYONE_EXCEPT_HOURLY_PAID:
										if (StringUtils.hasLength(configuration.getModules().getAccountCreation().getHourlyWageCode()) &&
											 configuration.getModules().getAccountCreation().getHourlyWageCode().equals(affiliation.getEmploymentTerms())) {
											break;
										}

										return true;
								}
							}
						}
						break;
				}
			}
		}

		return false;
	}

	@Transactional(rollbackFor = Exception.class)
	public void nightlyJob() {
		log.info("Starting nightly job");

		// get all affiliations that are relevant and not stopped
		List<String> masters = configuration.getScheduled().getAccountOrderGeneration().getMasters();
		List<Affiliation> affiliations = affiliationService.findAll();
		affiliations = affiliations.stream()
				.filter(a -> masters.contains(a.getMaster()) && a.getPerson().isDisableAccountOrders() == false)
				.collect(Collectors.toList());
		affiliations = AffiliationService.notStoppedAffiliations(affiliations);

		/// HANDLE CREATE ORDERS ///

		// NOTE: we are setting the "takeExistingAccounts" flag to false, so we ensure a clean set of
		//       orders, so any changes to the dataset (affiliations mostly) will result in old (unprocessed)
		//       orders being removed.
		List<AccountOrder> newCreateOrders = getAccountsToCreate(affiliations, false, true);

		// remove any duplicate new orders
		List<AccountOrder> distinctCreateOrders = new ArrayList<>();
		newCreateOrders.forEach(newOrder -> {if(distinctCreateOrders.stream().noneMatch(distinctOrder -> distinctOrder.logicalEquals(newOrder))){distinctCreateOrders.add(newOrder);}});

		List<AccountOrder> existingOrders = findAllCreateOrders();

		// remove existing create orders that are no longer relevant
		int removedCreateOrders = 0;
		for (AccountOrder existingOrder : existingOrders) {

			// manual orders are not removed by the nightly job
			if (existingOrder.getRequesterUuid() != null) {
				continue;
			}
			
			// only PENDING should be removed - we keep the CREATED/FAILED orders
			if (!existingOrder.getStatus().equals(AccountOrderStatus.PENDING)) {
				continue;
			}

			boolean noMatch = distinctCreateOrders.stream()
					 .noneMatch(newOrder -> newOrder.getPersonUuid().equals(existingOrder.getPersonUuid()) &&
							 				Objects.equals(newOrder.getEmployeeId(), existingOrder.getEmployeeId()) &&
							 				newOrder.getUserType().equals(existingOrder.getUserType()));

			if (noMatch) {
				removedCreateOrders++;
				delete(existingOrder);
			}
		}

		// new create orders to be added to able (do not order if an existing 
		int addedCreateOrders = 0;
		for (AccountOrder newOrder : distinctCreateOrders) {
			boolean noMatch = existingOrders.stream()
					 .noneMatch(existingOrder -> newOrder.getPersonUuid().equals(existingOrder.getPersonUuid()) &&
							 					 Objects.equals(newOrder.getEmployeeId(), existingOrder.getEmployeeId()) &&
							 					 newOrder.getUserType().equals(existingOrder.getUserType()));

			if (noMatch) {
				addedCreateOrders++;
				save(newOrder);
			}
		}

		/// HANDLE DELETE/DEACTIVATE ORDERS ///

		List<AccountOrder> newDeleteOrders = getAccountsToDelete(personService.getActive(), true);		
		List<AccountOrder> existingDeleteOrders = findAllDeleteAndDeactivateOrders();

		// remove existing delete/deactivate (pending) orders that are no longer relevant
		int removedDeleteOrders = 0;
		for (AccountOrder existingOrder : existingDeleteOrders) {

			// manual orders are not removed by the nightly job
			if (existingOrder.getRequesterUuid() != null) {
				continue;
			}
			
			// the purpose is only to cleanup no-longer-relevant pending orders, so skip non-pending
			if (!existingOrder.getStatus().equals(AccountOrderStatus.PENDING)) {
				continue;
			}

			// delete orders on Active Directory accounts are not removed by the nightly job,
			// as the account might have been deactivated by a previous task-run, and then
			// this job would not know about the account, and thus not generate a delete job for it.
			if (existingOrder.getUserType().equals(SupportedUserTypeService.getActiveDirectoryUserType()) &&
				existingOrder.getOrderType().equals(AccountOrderType.DELETE)) {
				
				// we double-check against actual users, because the person might have been re-hired, and the old account re-activated,
				// and in that case, we do NOT want to delete the account
				User user = userService.findByUserIdAndUserType(existingOrder.getActualUserId(), SupportedUserTypeService.getActiveDirectoryUserType());
				if (user != null && user.isDisabled()) {
					continue;
				}				
			}

			boolean noMatch = newDeleteOrders.stream()
					 .noneMatch(newOrder -> newOrder.getPersonUuid().equals(existingOrder.getPersonUuid()) &&
							 				newOrder.getOrderType().equals(existingOrder.getOrderType()) &&
							 				Objects.equals(newOrder.getEmployeeId(), existingOrder.getEmployeeId()) &&
							 				newOrder.getUserType().equals(existingOrder.getUserType()));
			
			if (noMatch) {
				removedDeleteOrders++;
				delete(existingOrder);
			}
		}
		
		// new create orders to be added to able
		int addedDeleteOrders = 0;
		for (AccountOrder newOrder : newDeleteOrders) {
			boolean noMatch = existingDeleteOrders.stream()
					 .noneMatch(existingOrder -> newOrder.getPersonUuid().equals(existingOrder.getPersonUuid()) &&
							 					 newOrder.getOrderType().equals(existingOrder.getOrderType()) &&
							 					 Objects.equals(newOrder.getEmployeeId(), existingOrder.getEmployeeId()) &&
							 					 newOrder.getUserType().equals(existingOrder.getUserType()));

			if (noMatch) {
				addedDeleteOrders++;
				save(newOrder);
			}
		}
				
		log.info("Ordered the creation of " + addedCreateOrders + " accounts, the deacivation/deletion of " + addedDeleteOrders + " accounts, and cancelled " + (removedCreateOrders + removedDeleteOrders) + " orders");
	}

	@Transactional(rollbackFor = Exception.class)
	public void deletePendingCreateOrders(Person person) {
		accountOrderDao.deleteByStatusAndPersonUuidInAndOrderTypeIn(AccountOrderStatus.PENDING, Collections.singleton(person.getUuid()), Collections.singletonList(AccountOrderType.CREATE));
	}
	
	@Transactional(rollbackFor = Exception.class)
	public void deletePendingExpireOrders(Person person) {
		accountOrderDao.deleteByStatusAndPersonUuidInAndOrderTypeIn(AccountOrderStatus.PENDING, Collections.singleton(person.getUuid()), Collections.singletonList(AccountOrderType.EXPIRE));
	}
	
	// assumes that all orders are of the same userType and all have status PENDING
	public List<AccountOrder> identifyAndDeleteDuplicates(List<AccountOrder> pendingOrders) {
		List<AccountOrder> result = new ArrayList<>();
		
		if (pendingOrders == null || pendingOrders.size() == 0) {
			return pendingOrders;
		}
		
		SupportedUserType supportedUserType = supportedUserTypeService.findByKey(pendingOrders.get(0).getUserType());
		if (supportedUserType == null) {
			log.error("Unknown userType in identifyAndDeleteDuplicates: " + pendingOrders.get(0).getUserType());
			return pendingOrders;
		}
		
		for (AccountOrder order : pendingOrders) {
			boolean found = false;

			for (AccountOrder validatedOrder : result) {
				boolean orderTypeEquals = Objects.equals(validatedOrder.getOrderType(), order.getOrderType());
				boolean personEquals = Objects.equals(validatedOrder.getPersonUuid(), order.getPersonUuid());
				boolean userIdEquals = Objects.equals(validatedOrder.getRequestedUserId(), order.getRequestedUserId());
				
				boolean employeeIdEquals = true;
				if (!supportedUserType.isSingleUserMode()) {
					employeeIdEquals = Objects.equals(validatedOrder.getEmployeeId(), order.getEmployeeId());
				}

				if (orderTypeEquals && personEquals && userIdEquals && employeeIdEquals) {
					delete(order);
					found = true;

					break;
				}
			}
			
			if (!found) {
				result.add(order);
			}
		}

		return result;
	}

	public void notify(AccountOrder order) {
		Person person = personService.getByUuid(order.getPersonUuid());
		if (person == null) {
			log.error("Ordered AD account for person that does not exist: " + order.getPersonUuid() + " / " + order.getMessage());
			return;
		}
		
		// only send notifications for AD and EXCHANGE accounts
		if (!order.getUserType().equals(SupportedUserTypeService.getActiveDirectoryUserType()) &&
			!order.getUserType().equals(SupportedUserTypeService.getExchangeUserType())) {
			return;
		}
		
		// only send notifications for CREATE events for EXCHANGE accounts
		if (order.getUserType().equals(SupportedUserTypeService.getExchangeUserType()) &&
		   !order.getStatus().equals(AccountOrderStatus.CREATED)) {
			return;
		}

		switch (order.getStatus()) {
			case CREATED:
			case REACTIVATED: {
				Person manager = PersonService.getManager(person, order.getEmployeeId());

				if (order.getUserType().equals(SupportedUserTypeService.getActiveDirectoryUserType())) {
					if (!accountWasRecentlyEnabled(order)) {
						// notify manager through email
						if (manager != null) {
							EmailTemplate template = emailTemplateService.findByTemplateType(EmailTemplateType.AD_CREATE_MANAGER);

							// constraint computation for substitutes
							String orgUnitUuid = null;
							if (StringUtils.hasLength(order.getEmployeeId())) {
								Affiliation affiliation = person.getAffiliations().stream().filter(a -> Objects.equals(a.getEmployeeId(), order.getEmployeeId())).findFirst().orElse(null);

								if (affiliation != null) {
									orgUnitUuid = affiliation.getOrgUnit().getUuid();
								}
							}
							
							for (EmailTemplateChild child : template.getChildren()) {
								if (child.isEnabled()) {
									List<String> emailRecipients = emailTemplateService.getManagerOrSubstitutes(child, manager, orgUnitUuid);

									String message = child.getMessage();
									message = message.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, PersonService.getName(person));
									message = message.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, PersonService.getName(manager));
									message = message.replace(EmailTemplateService.ACCOUNT_PLACEHOLDER, order.getActualUserId());
									
									String title = child.getTitle();
									title = title.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, PersonService.getName(person));
									title = title.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, PersonService.getName(manager));
									title = title.replace(EmailTemplateService.ACCOUNT_PLACEHOLDER, order.getActualUserId());
		
									for (String email : emailRecipients) {
										emailQueueService.queueEmail(email, title, message, child.getMinutesDelay(), child);
									}
								}
							}
						}
						
						// notify employee through e-boks
						EmailTemplate template = emailTemplateService.findByTemplateType(EmailTemplateType.AD_CREATE_EMPLOYEE);
						for (EmailTemplateChild child : template.getChildren()) {
							if (child.isEnabled()) {
								String message = child.getMessage();
								message = message.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, PersonService.getName(person));
								message = message.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, PersonService.getName(person));
								message = message.replace(EmailTemplateService.ACCOUNT_PLACEHOLDER, order.getActualUserId());
								
								String title = child.getTitle();
								title = title.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, PersonService.getName(person));
								title = title.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, PersonService.getName(person));
								title = title.replace(EmailTemplateService.ACCOUNT_PLACEHOLDER, order.getActualUserId());
								
								long delay = getEboksDelay(person, child);
								
								if (isAdAndExchangeEmailTemplateActive()) {
									SupportedUserType exchangeSettings = supportedUserTypeService.findByKey(SupportedUserTypeService.getExchangeUserType());
									long minutesDelay = exchangeSettings.getMinutesDelay() + delay + 3 * 60;
									
									emailQueueService.queueEboks(person.getCpr(), title, message, minutesDelay, child);
								}
								else {
									emailQueueService.queueEboks(person.getCpr(), title, message, delay, child);
								}
							}
						}
					}
					else {
						log.info("Account was recently disabled - not triggering mailflow: " + order.getActualUserId());
					}
				}
				else if (order.getUserType().equals(SupportedUserTypeService.getExchangeUserType())) {

					// notify person through email
					String email = order.getActualUserId();
					if (!StringUtils.hasLength(email)) {
						log.warn("Create event on Exchange did not contain actual email adress, so we fallback to a potential existing email account");
						email = PersonService.getEmail(person);
					}

					if (email != null) {
						EmailTemplate template = emailTemplateService.findByTemplateType(EmailTemplateType.EXCHANGE_CREATE_EMPLOYEE);
						for (EmailTemplateChild child : template.getChildren()) {
							if (child.isEnabled()) {
								String message = child.getMessage();
								message = message.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, PersonService.getName(person));
								message = message.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, PersonService.getName(person));
								message = message.replace(EmailTemplateService.ACCOUNT_PLACEHOLDER, order.getActualUserId());
	
								String title = child.getTitle();
								title = title.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, PersonService.getName(person));
								title = title.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, PersonService.getName(person));
								title = title.replace(EmailTemplateService.ACCOUNT_PLACEHOLDER, order.getActualUserId());
	
								emailQueueService.queueEmail(email, title, message, child.getMinutesDelay(), child);
							}
						}

						EmailTemplate additionalEmail = emailTemplateService.findByTemplateType(EmailTemplateType.EXCHANGE_CREATE_EMPLOYEE2);
						for (EmailTemplateChild child : additionalEmail.getChildren()) {
							if (child.isEnabled()) {
								String message = child.getMessage();
								message = message.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, PersonService.getName(person));
								message = message.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, PersonService.getName(person));
								message = message.replace(EmailTemplateService.ACCOUNT_PLACEHOLDER, order.getActualUserId());
								
								String title = child.getTitle();
								title = title.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, PersonService.getName(person));
								title = title.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, PersonService.getName(person));
								title = title.replace(EmailTemplateService.ACCOUNT_PLACEHOLDER, order.getActualUserId());
	
								emailQueueService.queueEmail(email, title, message, child.getMinutesDelay(), child);
							}
						}
					}
					else {
						log.warn("Could not find any email to send the notification to when creating a new EXCHANGE account for person: " + person.getUuid());
					}
					
					// Notify person through eboks
					if (person.getCpr() != null) {
						if (isAdAndExchangeEmailTemplateActive()) {
							//Delete ad ebox messages from queue
							List<EmailQueue> messages = emailQueueService.getByCprAndDeliveryTtsAfter(person.getCpr(), new Date());
							emailQueueService.deleteAllIn(messages.stream().filter(m -> m.getEmailTemplateChild().getEmailTemplate().getTemplateType().equals(EmailTemplateType.AD_CREATE_EMPLOYEE)).collect(Collectors.toList()));
						}
						
						EmailTemplate template = emailTemplateService.findByTemplateType(EmailTemplateType.EXCHANGE_CREATE_EMPLOYEE_EBOKS);
						for (EmailTemplateChild child : template.getChildren()) {
							if (child.isEnabled()) {
								String message = child.getMessage();
								message = message.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, PersonService.getName(person));
								message = message.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, PersonService.getName(person));
								message = message.replace(EmailTemplateService.EXCHANGE_ACCOUNT_PLACEHOLDER, order.getActualUserId());
								message = message.replace(EmailTemplateService.ACCOUNT_PLACEHOLDER, order.getLinkedUserId());
								
								String title = child.getTitle();
								title = title.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, PersonService.getName(person));
								title = title.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, PersonService.getName(person));
								title = title.replace(EmailTemplateService.EXCHANGE_ACCOUNT_PLACEHOLDER, order.getActualUserId());
								title = title.replace(EmailTemplateService.ACCOUNT_PLACEHOLDER, order.getLinkedUserId());
								
								long delay = getEboksDelay(person, child);
								
								emailQueueService.queueEboks(person.getCpr(), title, message, delay, child);
							}
						}
					}
					else {
						log.warn("Could not find any cpr to send eboks message to for person: " + person.getUuid());
					}
				}

				break;
			}
			case FAILED: {
				// notify requester through email
				if (order.getRequesterUuid() != null) {
					Person requesterPerson = personService.getByUuid(order.getRequesterUuid());
					if (requesterPerson != null) {
						String email = PersonService.getEmail(requesterPerson);
						if (email != null) {
							EmailTemplate template = emailTemplateService.findByTemplateType(EmailTemplateType.AD_CREATE_FAILED);
							for (EmailTemplateChild child : template.getChildren()) {
								if (child.isEnabled()) {
									String message = child.getMessage();								
									message = message.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, PersonService.getName(requesterPerson));
									message = message.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, PersonService.getName(person));
									
									String title = child.getTitle();								
									title = title.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, PersonService.getName(requesterPerson));
									title = title.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, PersonService.getName(person));
		
									emailQueueService.queueEmail(email, title, message, child.getMinutesDelay(), child);
								}
							}
						}
					}
				}

				break;
			}
			case DEACTIVATED: {
				Person manager = PersonService.getManager(person, order.getEmployeeId());
				
				// notify manager through email
				if (manager != null) {
					EmailTemplate template = emailTemplateService.findByTemplateType(EmailTemplateType.AD_DISABLE_MANAGER);

					// constraint computation for substitutes
					String orgUnitUuid = null;
					if (StringUtils.hasLength(order.getEmployeeId())) {
						Affiliation affiliation = person.getAffiliations().stream().filter(a -> Objects.equals(a.getEmployeeId(), order.getEmployeeId())).findFirst().orElse(null);

						if (affiliation != null) {
							orgUnitUuid = affiliation.getOrgUnit().getUuid();
						}
					}
					
					for (EmailTemplateChild child : template.getChildren()) {
						if (child.isEnabled()) {
							List<String> emailRecipients = emailTemplateService.getManagerOrSubstitutes(child, manager, orgUnitUuid);
							
							String message = child.getMessage();
							message = message.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, PersonService.getName(manager));
							message = message.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, PersonService.getName(person));
							message = message.replace(EmailTemplateService.ACCOUNT_PLACEHOLDER, order.getActualUserId());
							
							String title = child.getTitle();
							title = title.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, PersonService.getName(manager));
							title = title.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, PersonService.getName(person));
							title = title.replace(EmailTemplateService.ACCOUNT_PLACEHOLDER, order.getActualUserId());

							for (String email : emailRecipients) {
								emailQueueService.queueEmail(email, title, message, child.getMinutesDelay(), child);
							}
						}
					}
				}

				break;
			}
			default:
				break; // do nothing
		}
	}

	// this method is for eboks messages. Here child.getMinutesDelay is days and not minutes :)
	private long getEboksDelay(Person person, EmailTemplateChild child) {
		long delay = 0;

		// only relevant for persons with ONE affiliation. If they already have other affiliations, it does
		// not matter that we send the e-boks message early
		if (person.getAffiliations().size() == 1) {
			Affiliation affiliation = person.getAffiliations().get(0);

			if (affiliation != null && affiliation.getStartDate() != null) {
				Date today = new Date();

				// child.getMinutesDelay = 0 means that the message should not be sent before the start date 
				if (AffiliationService.notActiveYet(affiliation, (int) child.getMinutesDelay())) {
					// the affiliation starts in more than x (child.getMinutesDelay()) days
					Date startDate = affiliation.getStartDate();
					Calendar cal = Calendar.getInstance();
					cal.setTime(startDate);
					cal.add(Calendar.DATE, (int) (-1 * child.getMinutesDelay()));
					Date dateToSend = cal.getTime();

					long diff = dateToSend.getTime() - today.getTime();
					delay = TimeUnit.MILLISECONDS.toMinutes(diff);
					
					// should not happen
					if (delay < 0) {
						delay = 0;
					}
				} 
			}
		}

		return delay;
	}
	
	private boolean accountWasRecentlyEnabled(AccountOrder order) {
		long days = settingService.getLongValueByKey(CustomerSetting.USER_INACTIVE_PERIOD);

		int intDays = (int) days;
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.DATE, -intDays);
		Date xDaysBefore = cal.getTime();
		
		@SuppressWarnings("deprecation")
		List<UserAudRow> result = jdbcTemplate.query(SELECT_AUD_USERS, new Object[] { order.getActualUserId(), xDaysBefore.getTime() }, (RowMapper<UserAudRow>) (rs, rownum) -> {
			UserAudRow userAudRow = new UserAudRow();

			userAudRow.setDisabled(rs.getBoolean("disabled"));
			userAudRow.setTimestamp(rs.getLong("timestamp"));

			return userAudRow;
		});

		// don't notify if the account has been both inactive and active within the last x days
		if (result.stream().filter(u -> u.isDisabled()).count() > 0 &&
			result.stream().filter(u -> !u.isDisabled()).count() > 0) {

			return true;
		}

		return false;
	}

	private boolean isAdAndExchangeEmailTemplateActive() {
		EmailTemplate ad = emailTemplateService.findByTemplateType(EmailTemplateType.AD_CREATE_EMPLOYEE);
		EmailTemplate exchange = emailTemplateService.findByTemplateType(EmailTemplateType.EXCHANGE_CREATE_EMPLOYEE_EBOKS);

		long adActive = ad.getChildren().stream().filter(c -> c.isEnabled()).count();
		long exchangeActive = exchange.getChildren().stream().filter(c -> c.isEnabled()).count();

		if (adActive > 0 && exchangeActive > 0) {
			return true;
		}

		return false;
	}
	
	public AccountOrder findByDependsOn(AccountOrder dependsOn) {
		return accountOrderDao.findByDependsOn(dependsOn);
	}
}
