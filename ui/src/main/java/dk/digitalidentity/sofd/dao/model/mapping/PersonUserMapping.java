package dk.digitalidentity.sofd.dao.model.mapping;

import org.hibernate.envers.Audited;

import dk.digitalidentity.sofd.dao.model.MasteredEntity;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.User;
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
@Entity(name = "persons_users")
@Getter
@Setter
public class PersonUserMapping extends MappedEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "person_uuid")
	@NotNull
	private Person person;

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "user_id")
	@NotNull
	private User user;

	@Override
	public MasteredEntity getEntity() {
		return user;
	}
}
