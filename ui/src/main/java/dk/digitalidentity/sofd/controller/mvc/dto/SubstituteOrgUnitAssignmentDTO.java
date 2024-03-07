package dk.digitalidentity.sofd.controller.mvc.dto;

import dk.digitalidentity.sofd.dao.model.SubstituteOrgUnitAssignment;
import dk.digitalidentity.sofd.service.PersonService;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubstituteOrgUnitAssignmentDTO {
	private long id;
	private String substituteName;
	private String substituteContext;
	private boolean inherited;
	private String inheritedFrom;

	public SubstituteOrgUnitAssignmentDTO(SubstituteOrgUnitAssignment subAssignment) {
		this.id = subAssignment.getId();
		this.substituteName = PersonService.getName(subAssignment.getSubstitute());
		this.substituteContext = subAssignment.getContext().getName();
	}

	public SubstituteOrgUnitAssignmentDTO(SubstituteOrgUnitAssignment subAssignment, boolean inherited, String inheritedFrom) {
		this.id = subAssignment.getId();
		this.substituteName = PersonService.getName(subAssignment.getSubstitute());
		this.substituteContext = subAssignment.getContext().getName();
		this.inherited = inherited;
		this.inheritedFrom = inheritedFrom;
	}
}
