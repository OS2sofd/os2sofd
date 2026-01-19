package dk.digitalidentity.sofd.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import dk.digitalidentity.sofd.dao.model.enums.UsernameInfixType;
import dk.digitalidentity.sofd.dao.model.enums.UsernamePrefixType;
import dk.digitalidentity.sofd.dao.model.enums.UsernameSuffixType;
import dk.digitalidentity.sofd.dao.model.enums.UsernameType;
import lombok.Getter;
import lombok.Setter;


@Entity(name = "supported_user_types")
@Getter
@Setter
public class SupportedUserType {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(name = "unique_key")
	@NotNull
	@Size(max = 255)
	private String key;

	@Column
	@NotNull
	@Size(max = 255)
	private String name;
	
	@Column
	private boolean canOrder;
	
	@Column
	private boolean singleUserMode;

	@Column
	private long daysBeforeToCreate;

	@Column
	private long daysToDeactivate;
	
	@Column
	private long daysToDelete;

	@ManyToOne
	@JoinColumn(name = "depends_on")
	private SupportedUserType dependsOn;
	
	@Column
	private long minutesDelay;
	
	@Column
	@Enumerated(EnumType.STRING)
	private UsernameType usernameType;
	
	@Column
	@Enumerated(EnumType.STRING)
	private UsernamePrefixType usernamePrefix;
	
	@Column
	private String usernamePrefixValue;
	
	@Column
	@Enumerated(EnumType.STRING)
	private UsernameInfixType usernameInfix;
	
	@Column
	private String usernameInfixValue;
	
	@Column
	@Enumerated(EnumType.STRING)
	private UsernameSuffixType usernameSuffix;
	
	@Column
	private String usernameSuffixValue;

	@Column
	private String usernamePrefixExternalValue;

	@Column
	private String usernameSuffixExternalValue;
	
	@Column
	private String usernameTemplateString;

	@Column
	private boolean deactivateEnabled;

	@Column
	private boolean deleteEnabled;

	@Column
	private boolean createEnabled;

}
