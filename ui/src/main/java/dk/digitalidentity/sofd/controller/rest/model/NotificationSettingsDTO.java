package dk.digitalidentity.sofd.controller.rest.model;

import dk.digitalidentity.sofd.dao.model.enums.NotificationType;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;

@Getter
@Setter
public class NotificationSettingsDTO {
	private boolean deleteAlreadyCreated;
	private HashMap<NotificationType, Boolean> notificationTypes;
	private Long userInactivePeriod;
	private Long daysBeforeFunctionAssignmentExpires;
}
