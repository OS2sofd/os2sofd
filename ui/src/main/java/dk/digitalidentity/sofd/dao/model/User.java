package dk.digitalidentity.sofd.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;
import org.springframework.data.annotation.ReadOnlyProperty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import dk.digitalidentity.sofd.serializer.LocalExtensionsDeserializer;
import dk.digitalidentity.sofd.serializer.LocalExtensionsSerializer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "users")
@Getter
@Setter
@Audited
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class User extends MasteredEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	@NotNull
	private String uuid;
	
	@Column
	@NotNull
	private String master;
	
	@Column
	@NotNull
	private String masterId;

	@Column
	@NotNull
	@Size(max = 64)
	private String userId;
	
	@Column
	@Size(max = 255)
	private String employeeId;

	@NotNull
	@Column
	private String userType;

	@Column
	@JsonSerialize(using = LocalExtensionsSerializer.class)
	@JsonDeserialize(using = LocalExtensionsDeserializer.class)
	private String localExtensions;

	// we want to be able to read this information, but we do NOT want to update it through the person/user construct,
	// so a readonly annotation is required here
	@ReadOnlyProperty
	@BatchSize(size = 100)
	@OneToOne(mappedBy = "user")
	private ActiveDirectoryDetails activeDirectoryDetails;
	
	@Column
	@NotNull
	private boolean prime;

	@Column
	@NotNull
	private boolean substituteAccount;

	@Column
	@NotNull
	private boolean disabled;
	
	// TODO: temporary hack to ensure we can deal with null values in Better API
	private transient Boolean tSubstituteAccount;
	private transient Boolean tDisabled;
}
