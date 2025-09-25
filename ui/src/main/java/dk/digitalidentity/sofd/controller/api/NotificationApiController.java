package dk.digitalidentity.sofd.controller.api;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.dao.model.Notification;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.NotificationType;
import dk.digitalidentity.sofd.security.RequireApiWriteAccess;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.service.NotificationService;

@RestController
public class NotificationApiController {

	@Autowired
	private NotificationService notificationService;
	
	private record NotificationRecord(
			String affectedEntityUuid,
			EntityType affectedEntityType,
			String affectedEntityName,
			NotificationType notificationType,
			String message,
			LocalDate eventDate,
			LocalDate created,
			boolean active
	) {}
	
	private LocalDate convertToLocalDateViaInstant(Date dateToConvert) {
		if (dateToConvert == null) {
			return null;
		}

	    return dateToConvert.toInstant()
	      .atZone(ZoneId.systemDefault())
	      .toLocalDate();
	}

	@RequireReadAccess
	@GetMapping(value = "/api/notifications/{notificationType}")
	public ResponseEntity<?> getNotifications(@PathVariable("notificationType") NotificationType notificationType) {
		List<Notification> notifications = notificationService.findAllByType(notificationType);
		List<NotificationRecord> result = notifications.stream()
				.map(n -> new NotificationRecord(
						n.getAffectedEntityUuid(),
						n.getAffectedEntityType(),
						n.getAffectedEntityName(),
						n.getNotificationType(),
						n.getMessage(),
						n.getEventDate(),
						convertToLocalDateViaInstant(n.getCreated()),
						n.isActive()))
				.collect(Collectors.toList());

		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@RequireApiWriteAccess
	@PostMapping(value = "/api/notifications")
	public ResponseEntity<?> postNotification(@RequestBody NotificationRecord[] notificationRecord, @RequestParam(name = "fullSync", required = false, defaultValue = "false") boolean fullSync) {
		List<Notification> currentNotifications = notificationService.findAll();
		ArrayList<Notification> newNotifications = new ArrayList<>();

		for (int i = 0; i < notificationRecord.length; i++) {
			Notification notification = new Notification();
			notification.setAffectedEntityName(notificationRecord[i].affectedEntityName);
			notification.setAffectedEntityUuid(notificationRecord[i].affectedEntityUuid);
			notification.setAffectedEntityType(notificationRecord[i].affectedEntityType);
			notification.setNotificationType(notificationRecord[i].notificationType);
			notification.setMessage(notificationRecord[i].message);
			notification.setEventDate(notificationRecord[i].eventDate);
			notification.setActive(true);
			notification.setCreated(new Date());
			notification.setLastUpdated(notification.getCreated());

			if (!checkIfExists(currentNotifications, notification)) {
				newNotifications.add(notification);
			}
			else {
				remove(currentNotifications, notification);
			}
		}

		notificationService.saveAll(newNotifications);

		// in case of a full sync, remove any in the database that did not match the existing ones
		if (fullSync) {
			notificationService.deleteAll(currentNotifications);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	private boolean checkIfExists(List<Notification> old, Notification notification) {
		if (old.stream().anyMatch(o -> isEqual(o, notification))) {
			return true;
		}

		return false;
	}
	
	private void remove(List<Notification> old, Notification notification) {
		for (Iterator<Notification> iterator = old.iterator(); iterator.hasNext();) {
			Notification notification2 = iterator.next();
			
			if (isEqual(notification, notification2)) {
				iterator.remove();
				break;
			}
		}
	}
	
	private boolean isEqual(Notification notification1, Notification notification2) {
		return Objects.equals(notification1.getAffectedEntityUuid(), notification2.getAffectedEntityUuid()) &&
			   Objects.equals(notification1.getMessage(), notification2.getMessage()) &&
			   Objects.equals(notification1.getNotificationType(), notification2.getNotificationType()) &&
			   Objects.equals(notification1.getAffectedEntityName(), notification2.getAffectedEntityName());
	}
}
