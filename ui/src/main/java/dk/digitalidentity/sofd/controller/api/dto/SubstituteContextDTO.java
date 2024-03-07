package dk.digitalidentity.sofd.controller.api.dto;

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
	private boolean assignableToOrgunit;
	private boolean inherit;
}
