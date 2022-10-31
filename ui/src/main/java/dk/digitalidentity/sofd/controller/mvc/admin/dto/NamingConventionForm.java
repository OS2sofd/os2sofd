package dk.digitalidentity.sofd.controller.mvc.admin.dto;

import dk.digitalidentity.sofd.service.model.RequestEmailType;
import dk.digitalidentity.sofd.service.model.RequestOpusType;
import dk.digitalidentity.sofd.service.model.UsernameStrategy;
import dk.digitalidentity.sofd.service.model.UsernameSuffix;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NamingConventionForm {
	// Active Directory
	private UsernameStrategy usernameStrategy;
	private String usernamePrefix;
	private UsernameSuffix usernameSuffix;
	private long usernameLength;
	
	// Exchange
	private RequestEmailType requestEmailType;
	private long requestEmailLength;
	
	// OPUS
	private RequestOpusType requestOpusType;
	private long requestOpusLength;
}