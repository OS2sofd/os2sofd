package dk.digitalidentity.sofd.controller.rest.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubstituteAssignmentAddDTO {
	private String substitute;
	private String person;
	private long context;
	private String[] constraint;
}
