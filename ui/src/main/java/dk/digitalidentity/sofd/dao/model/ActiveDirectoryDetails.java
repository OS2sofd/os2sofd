package dk.digitalidentity.sofd.dao.model;

import java.time.LocalDate;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import org.hibernate.envers.Audited;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;

import lombok.Getter;
import lombok.Setter;

@Entity(name = "active_directory_details")
@Getter
@Setter
@Audited
public class ActiveDirectoryDetails {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private long userId;
	
	// this allows us to map to a non-pk from the User side (i.e. the "userId" field on this end),
	// without having an actual back-reference that Hibernate will perform cascade-saving on, and
	// we can leave it NULL for most use-cases except when reading (which will work automatically
	// with the annotation on the other side) - hackish, but it does the trick... the trick btw
	// is to avoid entries in person_aud and user_aud whenever these fields are updated, which can
	// happen very often due to local scripting on the municipality side
	@OneToOne
	@JoinColumn(name = "userId", insertable = false, updatable = false)
	private User user;
	
	@Column
	private String userType;
	
	@Column
	@JsonSerialize(using = LocalDateSerializer.class)
	@JsonDeserialize(using = LocalDateDeserializer.class)
	private LocalDate passwordExpireDate;

	@Column
	@JsonSerialize(using = LocalDateSerializer.class)
	@JsonDeserialize(using = LocalDateDeserializer.class)
	private LocalDate accountExpireDate;

	@Column
	@JsonSerialize(using = LocalDateSerializer.class)
	@JsonDeserialize(using = LocalDateDeserializer.class)
	private LocalDate passwordLockedDate;
	
	@Column
	private boolean passwordLocked;

	@Column
	private String kombitUuid;
	
	@Column
	private String upn;
	
	@Column
	private String title;

	@Column
	@JsonSerialize(using = LocalDateSerializer.class)
	@JsonDeserialize(using = LocalDateDeserializer.class)
	private LocalDate whenCreated;
}
