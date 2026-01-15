package dk.digitalidentity.sofd.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;

import lombok.Getter;
import lombok.Setter;

@Entity(name = "authorization_code")
@Getter
@Setter
@Audited
@BatchSize(size = 100)
public class AuthorizationCode {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(nullable = false, length = 255)
	private String code;

	@Column(nullable = false, length = 255)
	private String name;

	@Column(nullable = false)
	private boolean prime;
}
