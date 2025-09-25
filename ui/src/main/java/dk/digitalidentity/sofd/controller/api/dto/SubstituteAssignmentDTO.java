package dk.digitalidentity.sofd.controller.api.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubstituteAssignmentDTO {
	private long id;
	private long substituteContextId;
	private String substituteContextName;
	private ManagerSubstitutePersonDTO manager;
	private ManagerSubstitutePersonDTO substitute;
	private List<OUConstraintDTO> constraintOrgUnits;
	private boolean orgUnitAssignment = false;
}
