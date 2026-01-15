package dk.digitalidentity.sofd.dao.model.mapping;

import org.hibernate.envers.Audited;

import dk.digitalidentity.sofd.dao.model.MasteredEntity;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Post;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Audited
@Entity(name = "orgunits_posts")
@Getter
@Setter
public class OrgUnitPostMapping extends MappedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "orgunit_uuid")
	@NotNull
	private OrgUnit orgUnit;

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "post_id")
	@NotNull
	private Post post;

	@Override
	public MasteredEntity getEntity() {
		return post;
	}
}
