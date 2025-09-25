package dk.digitalidentity.sofd.dao.model.mapping;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;

import org.hibernate.envers.Audited;

import dk.digitalidentity.sofd.dao.model.MasteredEntity;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.User;
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
