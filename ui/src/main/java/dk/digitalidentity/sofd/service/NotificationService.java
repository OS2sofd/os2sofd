package dk.digitalidentity.sofd.service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.NotificationDao;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.Notification;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.OrgUnitAccountOrder;
import dk.digitalidentity.sofd.dao.model.OrgUnitAccountOrderType;
import dk.digitalidentity.sofd.dao.model.OrgUnitAccountOrderTypePosition;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SupportedUserType;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.NotificationType;

@Service
public class NotificationService {

	@Autowired
	private NotificationDao notificationDao;
	
	@Autowired
	private SupportedUserTypeService userTypeService;
	
	@Autowired
	private AccountOrderService accountOrderService;
	
	@Autowired
	private OrgUnitService orgUnitService;
	
	@Autowired
	private PersonService personService;
	
	@Autowired
	private SupportedUserTypeService supportedUserTypeService;

	@Autowired
	private SettingService settingService;
	
	@Autowired
	private SofdConfiguration configuration;
	
	public long countActive() {
		return notificationDao.countByActiveTrue();
	}
	
	public List<Notification> findAll() {
		return notificationDao.findAll();
	}

	public Notification findById(long id) {
		return notificationDao.findById(id);
	}

	public Notification save(Notification notification) {
		// Only save if enabled in settings
		if (settingService.isNotificationTypeEnabled(notification.getNotificationType())) {
			// message comes from "user"-input, and might be longer than 1000
			if (notification.getMessage() != null && notification.getMessage().length() > 1000) {
				notification.setMessage(notification.getMessage().substring(0, 999));
			}
			
			return notificationDao.save(notification);
		}

		return null;
	}
	
	public long saveAll(Collection<Notification> notifications) {
		if (notifications == null || notifications.size() == 0) {
			return 0;
		}
		
		long count = 0;
		Map<NotificationType, List<Notification>> groupings = notifications.stream().collect(Collectors.groupingBy(Notification::getNotificationType));
		for (NotificationType notificationType : groupings.keySet()) {
			// Only save if enabled in settings
			if (settingService.isNotificationTypeEnabled(notificationType)) {
				List<Notification> toSave = groupings.get(notificationType);
				count += toSave.size();

				notificationDao.saveAll(toSave);
			}
		}

		return count;
	}
	
	public List<Notification> findAllByType(NotificationType type) {
		return notificationDao.findAllByNotificationType(type);
	}

	public void delete(Notification notification) {
		notificationDao.delete(notification);
	}
	
	public void deleteAll(List<Notification> notifications) {
		notificationDao.deleteAll(notifications);
	}

	@Transactional
	public void deleteAllByNotificationType(NotificationType type) {
		notificationDao.deleteByNotificationType(type);
	}

	@Transactional
	public long generateUsersNotSupportedByRuleNotifications() {
		List<SupportedUserType> supportedUserTypes = userTypeService.findAll().stream()
				.filter(u -> u.isCanOrder())
				.collect(Collectors.toList());
		
		// if nothing can be ordered, no events can be generated
		if (supportedUserTypes.size() == 0) {
			return 0;
		}

		Map<String, Notification> existingNotificationsMap = findAllByType(NotificationType.PERSON_WITH_DISALLOWED_ACCOUNT).stream().collect(Collectors.toMap(Notification::getAffectedEntityUuid, Function.identity()));
		List<Notification> notifications = new ArrayList<>();
		
		List<Person> persons = personService.getActive();
		for (Person person : persons) {
			if (person.getUsers() == null || person.getUsers().size() == 0) {
				continue;
			}
			
			// skip persons without active affiliations
			if (AffiliationService.onlyActiveAffiliations(person.getAffiliations()).size() == 0) {
				continue;
			}

			StringBuilder builder = null;
			for (SupportedUserType userType : supportedUserTypes) {

				// filter users on Type
				List<User> filteredUsers = person.onlyActiveUsers().stream().filter(u -> u.getUserType().equals(userType.getKey())).collect(Collectors.toList());
				
				// if OS2vikar is available, ignore all AD accounts with userId vikXXXX
				if (SupportedUserTypeService.isActiveDirectory(userType.getKey()) && configuration.getModules().getSubstitute().isEnabled()) {
					filteredUsers = filteredUsers.stream().filter(u -> !UserService.isSubstituteUser(u)).collect(Collectors.toList());
				}

				// if the person does not have any of these accounts, skip to next userType
				if (filteredUsers.size() == 0) {
					continue;
				}

				// the user has an account of this type - is it supported by a rule?
				boolean shouldOrder = false;
				for (Affiliation affiliation : person.getAffiliations()) {
					shouldOrder = accountOrderService.shouldOrderAccountOfType(userType.getKey(), affiliation, (int) userType.getDaysBeforeToCreate(), true);

					if (shouldOrder) {
						break;
					}
				}
				
				if (!shouldOrder) {
					if (builder == null) {
						builder = new StringBuilder("Følgende brugerkonti er ikke understøttet af bestillingsregler:\n\n");
					}

					for (User user : filteredUsers) {
						builder.append("'" + user.getUserId() + "' af typen '" + supportedUserTypeService.getPrettyName(user.getUserType()) + "'\n");
					}
				}
			}
			
			if (builder != null && !existingNotificationsMap.containsKey(person.getUuid())) {
				addNotification(notifications, builder.toString(), PersonService.getName(person), person.getUuid(), EntityType.PERSON, NotificationType.PERSON_WITH_DISALLOWED_ACCOUNT);
			}

			if (builder != null) {
				builder = null;
			}
			else {
				// cleanup old notifications (the accounts are now covered by rules)
				// TODO: what hapens if 2 accounts where not covered by rules, but then one of them suddenly is?
				//       we never cleanup the notification in that case, and the message stays the same
				if (existingNotificationsMap.containsKey(person.getUuid())) {
					Notification notification = existingNotificationsMap.get(person.getUuid());

					notificationDao.delete(notification);
				}
			}
		}
		
		if (notifications.size() > 0) {
			return saveAll(notifications);
		}
		
		return 0;
	}

	@Transactional
	public long generateADWithBadEmployeeIdNotifications() {
		Map<String, Notification> existingNotificationsMap = findAllByType(NotificationType.PERSON_WITH_AD_ACCOUNT_WITH_BAD_EMPLOYEEID).stream().collect(Collectors.toMap(Notification::getAffectedEntityUuid, Function.identity()));
		List<Notification> notifications = new ArrayList<Notification>();
		
		List<Person> persons = personService.findByUserType(SupportedUserTypeService.getActiveDirectoryUserType());
		for (Person person : persons) {
			boolean badEmployeeId = false;
			String message = null;

			for (User user : person.onlyActiveUsers()) {
				if (!SupportedUserTypeService.isActiveDirectory(user.getUserType())) {
					continue;
				}
				
				if (!StringUtils.hasLength(user.getEmployeeId())) {
					continue;
				}

				boolean match = AffiliationService.notStoppedAffiliations(person.getAffiliations()).stream().anyMatch(a -> Objects.equals(a.getEmployeeId(), user.getEmployeeId()));

				if (!match) {
					badEmployeeId = true;
					message = "AD kontoen '" + user.getUserId() + "' er knyttet til et tilhørsforhold som personen ikke har (" + user.getEmployeeId() + ")";
					break;
				}
			}

			boolean alreadyBad = existingNotificationsMap.containsKey(person.getUuid());
			if (badEmployeeId && !alreadyBad) {
				addNotification(notifications, message, PersonService.getName(person), person.getUuid(), EntityType.PERSON, NotificationType.PERSON_WITH_AD_ACCOUNT_WITH_BAD_EMPLOYEEID);
			}
			else if (!badEmployeeId && alreadyBad) {
				Notification notification = existingNotificationsMap.get(person.getUuid());
				notificationDao.delete(notification);
			}
		}
		
		// clean up potential notifications for people that no longer has at least one active AD account
		List<String> uuids = persons.stream().map(p -> p.getUuid()).collect(Collectors.toList());
		for (Entry<String, Notification> entry : existingNotificationsMap.entrySet()) {
			if (!uuids.contains(entry.getKey())) {
				notificationDao.delete(entry.getValue());
			}
		}

		if (notifications.size() > 0) {
			return saveAll(notifications);
		}
		
		return 0;
	}

	@Transactional
	public long generateFutureADWithBadEmployeeIdNotifications() {
		Map<String, Notification> existingNotificationsMap = findAllByType(NotificationType.PERSON_WITH_FUTURE_AD_ACCOUNT_WITH_BAD_EMPLOYEEID).stream().collect(Collectors.toMap(Notification::getAffectedEntityUuid, Function.identity()));
		List<Notification> notifications = new ArrayList<Notification>();

		List<Person> persons = personService.findByUserType(SupportedUserTypeService.getActiveDirectoryUserType());
		for (Person person : persons) {
			boolean futureBadEmployeeId = false;
			String message = null;

			for (User user : person.onlyActiveUsers()) {
				if (!SupportedUserTypeService.isActiveDirectory(user.getUserType())) {
					continue;
				}

				if (!StringUtils.hasLength(user.getEmployeeId())) {
					continue;
				}

				// we check if the affiliation mapped to this user is expiring within the next 3 days
				// if we pass the 3 days the other notification (ADWithBadEmployeeIdNotification) takes over
				var isExpiring = person.getAffiliations().stream().anyMatch(a ->
						Objects.equals(a.getEmployeeId(), user.getEmployeeId())
						&& (a.getStopDate() != null
							&& toLocalDate(a.getStopDate()).isBefore(LocalDate.now().plusDays(4))
							&& toLocalDate(a.getStopDate()).isAfter(LocalDate.now())
						));

				if( isExpiring )
				{
					// check if there is another active affiliation that is not mapped to a user
					var mappedEmployeeIds = person.onlyActiveUsers().stream().filter(u -> u.getEmployeeId() != null).map(u -> u.getEmployeeId()).toList();
					var otherAffiliationExists = AffiliationService.notStoppedAffiliations(person.getAffiliations()).stream().anyMatch(a ->
							!Objects.equals(a.getEmployeeId(), user.getEmployeeId())
							&& !mappedEmployeeIds.contains(a.getEmployeeId())
						);

					// we only add the notification if there is another affiliation that the user could be mapped to
					if (otherAffiliationExists) {
						futureBadEmployeeId = true;
						message = "AD kontoen '" + user.getUserId() + "' er knyttet til et tilhørsforhold som snart udløber (" + user.getEmployeeId() + "). Overvej at skifte tilhørsforholdet.";
						break;
					}
				}
			}

			boolean alreadyBad = existingNotificationsMap.containsKey(person.getUuid());
			if (futureBadEmployeeId && !alreadyBad) {
				addNotification(notifications, message, PersonService.getName(person), person.getUuid(), EntityType.PERSON, NotificationType.PERSON_WITH_FUTURE_AD_ACCOUNT_WITH_BAD_EMPLOYEEID);
			}
			else if (!futureBadEmployeeId && alreadyBad) {
				Notification notification = existingNotificationsMap.get(person.getUuid());
				notificationDao.delete(notification);
			}
		}

		// clean up potential notifications for people that no longer has at least one active AD account
		List<String> uuids = persons.stream().map(p -> p.getUuid()).collect(Collectors.toList());
		for (Entry<String, Notification> entry : existingNotificationsMap.entrySet()) {
			if (!uuids.contains(entry.getKey())) {
				notificationDao.delete(entry.getValue());
			}
		}

		if (notifications.size() > 0) {
			return saveAll(notifications);
		}

		return 0;
	}

	@Transactional
	public long generateDeletedParentOrgUnitNotifications() {

		Map<String, Notification> existingNotificationsMap = findAllByType(NotificationType.DELETED_PARENT_ORGUNIT).stream().collect(Collectors.toMap(Notification::getAffectedEntityUuid, Function.identity()));
		List<Notification> notifications = new ArrayList<Notification>();

		var deletedAncestorUnits = orgUnitService.getActiveWhereAncestorDeleted();

		// remove notifications that are no longer relevant
		for (var existingNotificationEntry : existingNotificationsMap.entrySet()) {
			if( deletedAncestorUnits.stream().noneMatch(o -> o.getUuid().equalsIgnoreCase(existingNotificationEntry.getKey()) ) ) {
				notificationDao.delete(existingNotificationEntry.getValue());
			}
		}

		// add new notifications
		for (var deletedAncestorUnit : deletedAncestorUnits) {
			if( !existingNotificationsMap.containsKey(deletedAncestorUnit.getUuid()) ) {
				var message = "Enheden '" + deletedAncestorUnit.getName() + "' (" + deletedAncestorUnit.getUuid() + ") ligger under en forældreenhed som er blevet slettet. Du kan fremsøge enheden i søgefeltet i højre hjørne og rette den overliggende enhed";
				addNotification(notifications, message, deletedAncestorUnit.getName(), deletedAncestorUnit.getUuid(), EntityType.ORGUNIT, NotificationType.DELETED_PARENT_ORGUNIT);
			}
		}

		if (notifications.size() > 0) {
			return saveAll(notifications);
		}
		return 0;
	}


	@Transactional
	public long generateMissingRulesNotifications() {
		List<String> supportedUserTypes = userTypeService.findAll().stream()
				.filter(u -> u.isCanOrder())
				.map(u -> u.getKey())
				.collect(Collectors.toList());
		
		// if nothing can be ordered, no events can be generated
		if (supportedUserTypes.size() == 0) {
			return 0;
		}
		
		Map<String, Notification> existingNotificationsMap = findAllByType(NotificationType.ORGUNIT_WITH_MISSING_RULES).stream().collect(Collectors.toMap(Notification::getAffectedEntityUuid, Function.identity()));
		List<Notification> notifications = new ArrayList<Notification>();
		StringBuilder builder = null;
		
		for (OrgUnit orgUnit : orgUnitService.getAllActiveWithAffiliations()) {
			OrgUnitAccountOrder accountOrder = accountOrderService.getAccountOrderSettings(orgUnit, false);
			boolean shouldNotify = false;

			for (OrgUnitAccountOrderType type : accountOrder.getTypes()) {
				if (!supportedUserTypes.contains(type.getUserType())) {
					continue;
				}

				boolean missingRule = false;

				switch (type.getRule()) {
					case DISABLED:
					case EVERYONE:
					case EVERYONE_EXCEPT_HOURLY_PAID:
					case BY_POSITION_NAME:
						// ok, no notification needed
						break;
					case UNDECIDED:
						if (!shouldNotify) {
							builder = new StringBuilder();
							shouldNotify = true;
						}
						missingRule = true;
						break;
				}
				
				if (missingRule) {
					builder.append("Mangler regler for '" + supportedUserTypeService.getPrettyName(type.getUserType()) + "'\n");
				}
			}

			if (shouldNotify && !existingNotificationsMap.containsKey(orgUnit.getUuid())) {
				addNotification(notifications, builder.toString(), orgUnit.getName(), orgUnit.getUuid(), EntityType.ORGUNIT, NotificationType.ORGUNIT_WITH_MISSING_RULES);
			}
			else if (!shouldNotify && existingNotificationsMap.containsKey(orgUnit.getUuid())) {
				Notification notification = existingNotificationsMap.get(orgUnit.getUuid());
				
				notificationDao.delete(notification);
			}
		}
		
		if (notifications.size() > 0) {
			return saveAll(notifications);
		}
		
		return 0;
	}
	
	@Transactional
	public long generateMissingRulesTitlesNotifications() {
		List<String> supportedUserTypes = userTypeService.findAll().stream()
				.filter(u -> u.isCanOrder())
				.map(u -> u.getKey())
				.collect(Collectors.toList());
		
		// if nothing can be ordered, no events can be generated
		if (supportedUserTypes.size() == 0) {
			return 0;
		}
		
		Map<String, Notification> existingNotificationsMap = findAllByType(NotificationType.ORGUNIT_WITH_MISSING_RULES_TITLES).stream().collect(Collectors.toMap(Notification::getAffectedEntityUuid, Function.identity()));
		List<Notification> notifications = new ArrayList<Notification>();
		StringBuilder builder = null;
		
		for (OrgUnit orgUnit : orgUnitService.getAllActiveWithAffiliations()) {
			OrgUnitAccountOrder accountOrder = accountOrderService.getAccountOrderSettings(orgUnit, false);
			boolean shouldNotify = false;

			for (OrgUnitAccountOrderType type : accountOrder.getTypes()) {
				if (!supportedUserTypes.contains(type.getUserType())) {
					continue;
				}

				boolean missingRule = false;

				switch (type.getRule()) {
					case DISABLED:
					case EVERYONE:
					case EVERYONE_EXCEPT_HOURLY_PAID:
					case UNDECIDED:
						// ok, no notification needed
						break;
					case BY_POSITION_NAME:
						for (OrgUnitAccountOrderTypePosition position : type.getPositions()) {
							switch (position.getRule()) {
								case BY_POSITION_NAME:
								case DISABLED:
								case EVERYONE:
								case EVERYONE_EXCEPT_HOURLY_PAID:
									// ok, no notification needed
									break;
								case UNDECIDED:
									if (!shouldNotify) {
										builder = new StringBuilder();
										shouldNotify = true;
									}
									missingRule = true;									
									break;
							}
						}
						break;
				}
				
				if (missingRule) {
					builder.append("Mangler regler for '" + supportedUserTypeService.getPrettyName(type.getUserType()) + "'\n");
				}
			}

			if (shouldNotify && !existingNotificationsMap.containsKey(orgUnit.getUuid())) {
				addNotification(notifications, builder.toString(), orgUnit.getName(), orgUnit.getUuid(), EntityType.ORGUNIT, NotificationType.ORGUNIT_WITH_MISSING_RULES_TITLES);
			}
			else if (!shouldNotify && existingNotificationsMap.containsKey(orgUnit.getUuid())) {
				Notification notification = existingNotificationsMap.get(orgUnit.getUuid());
				
				notificationDao.delete(notification);
			}
		}
		
		if (notifications.size() > 0) {
			return saveAll(notifications);
		}
		
		return 0;
	}
	
	private void addNotification(List<Notification> notifications, String message, String name, String uuid, EntityType entityType, NotificationType notificationType) {
		Notification notification = new Notification();
		notification.setActive(true);
		notification.setAffectedEntityName(name);
		notification.setAffectedEntityType(entityType);
		notification.setAffectedEntityUuid(uuid);
		notification.setMessage(message);
		notification.setCreated(new Date());
		notification.setNotificationType(notificationType);

		notifications.add(notification);
	}

	@Transactional(rollbackFor = Exception.class)
	public void expire() {
		Date today = new Date();
		Calendar cal = Calendar.getInstance();
		List<Notification> notifications = findAll();
		ArrayList<Notification> expiredNotifications = new ArrayList<>();

		for (Notification notification : notifications) {
			int expireDays = notification.getNotificationType().getExpire();
			if (expireDays == 0) {
				continue; // notification never expires
			}

			cal.setTime(notification.getCreated());
			cal.add(Calendar.DATE, expireDays);
			Date expiryDate = cal.getTime();

			if (today.after(expiryDate)) {
				expiredNotifications.add(notification);
			}
		}

		notificationDao.deleteAll(expiredNotifications);
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
}
