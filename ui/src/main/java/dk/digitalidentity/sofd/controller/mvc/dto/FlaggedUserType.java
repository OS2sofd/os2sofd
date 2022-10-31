package dk.digitalidentity.sofd.controller.mvc.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FlaggedUserType {
	private String userType;
	private String userTypeName;
	private boolean flagged;
}
