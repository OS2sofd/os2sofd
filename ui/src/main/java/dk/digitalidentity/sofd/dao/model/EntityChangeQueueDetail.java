package dk.digitalidentity.sofd.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

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
