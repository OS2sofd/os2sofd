package dk.digitalidentity.sofd.dao.model;

import java.util.Date;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "entity_change_queue")
public class EntityChangeQueue {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private String entityType;

	@Column
	private String entityUuid;

	@Column
	private String changeType;

	@OneToMany(mappedBy = "entityChangeQueue", cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
	private List<EntityChangeQueueDetail> entityChangeQueueDetails;

	@Column
	private Date tts;
}
