package dk.digitalidentity.sofd.dao.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.EventType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "audit_log", indexes = {@Index(name = "idx_audit_log_user_id", columnList = "user_id")})
public class AuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(nullable = false)
	private Date timestamp;

	@Column(nullable = false, name = "user_id")
	private String userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EntityType entityType;

	@Column(nullable = true)
	private String entityId;
	
	@Column(nullable = true)
	private String entityName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private EventType eventType;
	
	@Column(nullable = true)
	private String message;
}