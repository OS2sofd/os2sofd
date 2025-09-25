package dk.digitalidentity.sofd.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

@Entity(name = "reserved_usernames")
@Getter
@Setter
public class ReservedUsername {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@Column
	@NotNull
	private String personUuid;
	
	@Column
	private String employeeId;

	@Column
	@NotNull
	private String userType;

	@Column
	@NotNull
	private String userId;

}
