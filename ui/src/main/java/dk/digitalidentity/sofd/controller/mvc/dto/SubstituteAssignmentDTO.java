package dk.digitalidentity.sofd.controller.mvc.dto;

import java.util.ArrayList;
import java.util.List;

import dk.digitalidentity.sofd.dao.model.SubstituteAssignment;
import dk.digitalidentity.sofd.dao.model.SubstituteAssignmentOrgUnitMapping;
import dk.digitalidentity.sofd.service.PersonService;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubstituteAssignmentDTO {
	private long id;
	private String substituteName;
	private String substituteContext;
	private List<String> orgUnits;

	public SubstituteAssignmentDTO(SubstituteAssignment subAssignment) {
		this.id = subAssignment.getId();
		this.substituteName = PersonService.getName(subAssignment.getSubstitute());
		this.substituteContext = subAssignment.getContext().getName();
		
		this.orgUnits = new ArrayList<>();
		for (SubstituteAssignmentOrgUnitMapping mapping : subAssignment.getConstraintMappings()) {
			orgUnits.add(mapping.getOrgUnit().getName());
		}
	}
}
