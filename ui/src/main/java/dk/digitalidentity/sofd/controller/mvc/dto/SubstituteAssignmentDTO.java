package dk.digitalidentity.sofd.controller.mvc.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dk.digitalidentity.sofd.dao.model.SubstituteAssignment;
import dk.digitalidentity.sofd.dao.model.SubstituteAssignmentOrgUnitMapping;
import dk.digitalidentity.sofd.dao.model.SubstituteOrgUnitAssignment;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.model.SubstituteOrgUnitAssignmentDTO;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubstituteAssignmentDTO {
	private long id;
	private String substituteName;
	private String substituteContext;
	private List<String> orgUnits;
	private boolean orgUnitAssignment = false;
	private boolean inherited = false;
	private String inheritedFrom;

	public SubstituteAssignmentDTO(SubstituteAssignment subAssignment) {
		this.id = subAssignment.getId();
		this.substituteName = PersonService.getName(subAssignment.getSubstitute());
		this.substituteContext = subAssignment.getContext().getName();
		
		this.orgUnits = new ArrayList<>();
		for (SubstituteAssignmentOrgUnitMapping mapping : subAssignment.getConstraintMappings()) {
			orgUnits.add(mapping.getOrgUnit().getName());
		}
	}

	public SubstituteAssignmentDTO(SubstituteOrgUnitAssignmentDTO assignment) {
		this.id = assignment.getId();
		this.substituteName = assignment.getSubstituteName();
		this.substituteContext = assignment.getSubstituteContext();
		this.orgUnitAssignment = true;
		this.orgUnits = Collections.singletonList(assignment.getInheritedFrom());
	}

	public SubstituteAssignmentDTO(SubstituteOrgUnitAssignment assignment, String managedOrgUnitName, boolean inherited, String inheritedFrom) {
		this.id = assignment.getId();
		this.substituteName = PersonService.getName(assignment.getSubstitute());
		this.substituteContext = assignment.getContext().getName();
		this.orgUnitAssignment = true;
		this.orgUnits = Collections.singletonList(managedOrgUnitName);
		this.inherited = inherited;
		this.inheritedFrom = inheritedFrom;
	}
}
