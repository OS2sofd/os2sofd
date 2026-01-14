package dk.digitalidentity.sofd.dao.model.mapping;

import org.hibernate.envers.Audited;

import dk.digitalidentity.sofd.dao.model.AuthorizationCode;
import dk.digitalidentity.sofd.dao.model.Person;
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

@Entity(name = "persons_authorization_codes")
@Getter
@Setter
@Audited
public class PersonAuthorizationCodeMapping {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "person_uuid")
	@NotNull
	private Person person;

	@OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	@JoinColumn(name = "authorization_code_id")
	@NotNull
	private AuthorizationCode authorizationCode;
}
