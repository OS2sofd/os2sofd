package dk.digitalidentity.sofd.controller.mvc.datatables.dao.model;

import java.util.Date;

import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.EventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "view_datatables_auditlogs")
public class AuditLogView {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private Date timestamp;
	
	@Column
	private String userId;

	@Enumerated(EnumType.STRING)
	@Column
	private EntityType entityType;
	
	@Column
	private String entityName;

	@Enumerated(EnumType.STRING)
	@Column
	private EventType eventType;

	@Column
	private String message;
}