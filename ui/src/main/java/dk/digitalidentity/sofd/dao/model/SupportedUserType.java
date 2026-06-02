package dk.digitalidentity.sofd.dao.model;

import dk.digitalidentity.sofd.dao.model.enums.UsernameInfixType;
import dk.digitalidentity.sofd.dao.model.enums.UsernamePrefixType;
import dk.digitalidentity.sofd.dao.model.enums.UsernameSuffixType;
import dk.digitalidentity.sofd.dao.model.enums.UsernameType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
