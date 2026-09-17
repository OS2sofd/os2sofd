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

	// set on assignments that live on an orgUnit above the ones the person manages, and are inherited
	// down to them - they belong to a manager further up, and cannot be edited by this person
	private boolean inherited = false;
	private String inheritedFrom;
}
