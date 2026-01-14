package dk.digitalidentity.sofd.dao.model;

import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
