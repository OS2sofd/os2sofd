package dk.digitalidentity.sofd.config.properties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Affiliation {
	private boolean alternativeOUAssignable = false;
	private boolean externalDefaultInheritPrivileges = false;
}
