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

import dk.digitalidentity.sofd.dao.model.AuthorizationCode;
import dk.digitalidentity.sofd.dao.model.Person;
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
