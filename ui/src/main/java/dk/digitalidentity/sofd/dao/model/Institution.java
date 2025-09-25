package dk.digitalidentity.sofd.dao.model;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Audited
@Getter
@Setter
@Entity
public class Institution {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(nullable = false, length = 36)
	private String uuid;

	@Column(nullable = false)
	private String institutionNumber;

	@Column(nullable = false)
	private String name;

}
