package dk.digitalidentity.sofd.controller.mvc.datatables.dao.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.EventType;
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