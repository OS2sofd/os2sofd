package dk.digitalidentity.sofd.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
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

				// if the person does not have any of these accounts, skip to next userType
				if (!person.onlyActiveUsers().stream().anyMatch(u -> u.getUserType().equals(userType.getKey()))) {
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
					Optional<User> oUser = PersonService.getUsers(person).stream().filter(u -> u.getUserType().equals(userType.getKey())).findFirst();
					if (oUser.isPresent()) {
						if (builder == null) {
							builder = new StringBuilder("Følgende brugerkonti er ikke understøttet af bestillingsregler:\n\n");
						}
						
						builder.append("'" + oUser.get().getUserId() + "' af typen '" + supportedUserTypeService.getPrettyName(oUser.get().getUserType()) + "'\n");
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

				boolean match = AffiliationService.notStoppedAffiliations(person.getAffiliations()).stream()
						.anyMatch(a -> Objects.equals(a.getMaster(), configuration.getModules().getLos().getPrimeAffiliationMaster()) &&
									   Objects.equals(a.getEmployeeId(), user.getEmployeeId()));

				if (!match) {
					badEmployeeId = true;
					message = "AD kontoen '" + user.getUserId() + "' er knyttet til et medarbejder-id som personen ikke har (" + user.getEmployeeId() + ")";
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
}
