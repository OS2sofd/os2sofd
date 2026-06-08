package dk.digitalidentity.sofd.controller.mvc.dto;

import dk.digitalidentity.sofd.dao.model.enums.UsernameType;
import dk.digitalidentity.sofd.dao.model.enums.UsernameInfixType;
import dk.digitalidentity.sofd.dao.model.enums.UsernamePrefixType;
import dk.digitalidentity.sofd.dao.model.enums.UsernameSuffixType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SupportedUserTypeDTO {
	private long id;
	private boolean canOrder;
	private boolean singleUserMode;
	private long daysToDeactivate;
	private long daysToDelete;
	private long daysBeforeToCreate;
	private String key;
	private String name;
	private long dependsOn;
	private long dependsOnDelay;
	private UsernameType usernameType;
	private UsernamePrefixType prefix;
	private UsernameInfixType infix;
	private UsernameSuffixType suffix;
	private String prefixValue;
	private String infixValue;
	private String suffixValue;
	private String prefixExternalValue;
	private String suffixExternalValue;
	private String usernameTemplateString;
	private boolean deactivateEnabled;
	private boolean deleteEnabled;
	private boolean createEnabled;
}
