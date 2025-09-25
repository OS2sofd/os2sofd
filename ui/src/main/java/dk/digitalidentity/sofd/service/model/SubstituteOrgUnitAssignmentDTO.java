package dk.digitalidentity.sofd.service.model;

import dk.digitalidentity.sofd.dao.model.SubstituteOrgUnitAssignment;
import dk.digitalidentity.sofd.service.PersonService;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubstituteOrgUnitAssignmentDTO {
	private long id;
	private String substituteUuid;
	private String substituteName;
	private String substituteContextIdentifier;
	private String substituteContext;
	private boolean inherited;
	private String inheritedFrom;

	public SubstituteOrgUnitAssignmentDTO(SubstituteOrgUnitAssignment subAssignment) {
		this.id = subAssignment.getId();
		this.substituteUuid = subAssignment.getSubstitute().getUuid();
		this.substituteName = PersonService.getName(subAssignment.getSubstitute());
		this.substituteContextIdentifier = subAssignment.getContext().getIdentifier();
		this.substituteContext = subAssignment.getContext().getName();
		// todo consider renaming inheritedFrom. It's actually just "orgUnit" and is set here even if not inherited
		this.inheritedFrom = subAssignment.getOrgUnit().getName();
	}

	public SubstituteOrgUnitAssignmentDTO(SubstituteOrgUnitAssignment subAssignment, boolean inherited, String inheritedFrom) {
		this.id = subAssignment.getId();
		this.substituteUuid = subAssignment.getSubstitute().getUuid();
		this.substituteName = PersonService.getName(subAssignment.getSubstitute());
		this.substituteContextIdentifier = subAssignment.getContext().getIdentifier();
		this.substituteContext = subAssignment.getContext().getName();
		this.inherited = inherited;
		this.inheritedFrom = inheritedFrom;
	}
}
