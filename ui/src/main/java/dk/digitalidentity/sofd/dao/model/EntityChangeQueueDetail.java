package dk.digitalidentity.sofd.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import dk.digitalidentity.sofd.listener.EntityListenerService.ChangeType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "entity_change_queue_details")
public class EntityChangeQueueDetail {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "entity_change_queue_id")
	private EntityChangeQueue entityChangeQueue;

	@Column
	@NotNull
	@Enumerated(EnumType.STRING)
	private ChangeType changeType;

	@Column
	private String changeTypeDetails;
	
	@Column
	private String oldValue;
	
	@Column
	private String newValue;
}
