package dk.digitalidentity.sofd.dao.model;

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
import jakarta.persistence.Index;
import jakarta.persistence.Table;
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