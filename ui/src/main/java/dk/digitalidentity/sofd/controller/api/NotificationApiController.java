package dk.digitalidentity.sofd.controller.api;

import dk.digitalidentity.sofd.dao.model.Notification;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.NotificationType;
import dk.digitalidentity.sofd.security.RequireApiWriteAccess;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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
			LocalDate eventDate
	) {}
	
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
						n.getEventDate()))
				.collect(Collectors.toList());

		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@RequireApiWriteAccess
	@PostMapping(value = "/api/notifications")
	public ResponseEntity<?> postNotification(@RequestBody NotificationRecord[] notificationRecord) {
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
		}

		notificationService.saveAll(newNotifications);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	private boolean checkIfExists(List<Notification> old, Notification notification) {
		if (old.stream().anyMatch(o -> Objects.equals(o.getAffectedEntityUuid(), notification.getAffectedEntityUuid()) && Objects.equals(o.getMessage(),notification.getMessage()) &&
									   Objects.equals(o.getNotificationType(), notification.getNotificationType()) && Objects.equals(o.getAffectedEntityName(),notification.getAffectedEntityName()))) {
			return true;
		}

		return false;
	}
}
