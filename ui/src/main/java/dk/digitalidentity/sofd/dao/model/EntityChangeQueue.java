package dk.digitalidentity.sofd.dao.model;

import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

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
