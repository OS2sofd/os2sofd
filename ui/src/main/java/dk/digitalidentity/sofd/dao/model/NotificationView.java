package dk.digitalidentity.sofd.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.NotificationType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity(name = "view_notifications")
public class NotificationView {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column
	private long id;

	@Column
	private boolean active;

	@Column
	private String affectedEntityUuid;

	@Column
	@Enumerated(EnumType.STRING)
	private EntityType affectedEntityType;

	@Column
	private String affectedEntityName;

	@Column
	@Enumerated(EnumType.STRING)
	private NotificationType notificationType;

	@Column
	private String created;

	@Column
	private String eventDate;

	@Column
	private String lastUpdated;
	
	@Column
	private String message;

	@Column
	private String adminUuid;
	
	@Column
	private String adminName;
}
