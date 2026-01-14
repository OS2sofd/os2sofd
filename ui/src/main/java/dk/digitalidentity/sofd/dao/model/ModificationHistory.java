package dk.digitalidentity.sofd.dao.model;

import java.util.Date;

import org.hibernate.annotations.CreationTimestamp;

import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.service.model.ChangeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class ModificationHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@CreationTimestamp
	@Column(updatable = false)
	private Date changed;

	@Column
	private String uuid;

	@Column
	@Enumerated(EnumType.STRING)
	private EntityType entity;

	@Column
	@Enumerated(EnumType.STRING)
	private ChangeType changeType;
}
