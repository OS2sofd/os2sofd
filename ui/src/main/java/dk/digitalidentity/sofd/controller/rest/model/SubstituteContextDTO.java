package dk.digitalidentity.sofd.controller.rest.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubstituteContextDTO {
	private long id;
	private String name;
	private String identifier;
	private boolean supportsConstraints;
	private boolean canBeDeleted;
	private boolean assignableToOrgUnit;
	private boolean inherit;
}
