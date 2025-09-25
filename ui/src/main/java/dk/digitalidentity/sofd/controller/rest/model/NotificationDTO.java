package dk.digitalidentity.sofd.controller.rest.model;

import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationDTO {
	private long id;
	private boolean active;
	private String affectedEntityName;
	private String affectedEntityUuid;
	private EntityType affectedEntityType;
	private String adminTaskType;
}
