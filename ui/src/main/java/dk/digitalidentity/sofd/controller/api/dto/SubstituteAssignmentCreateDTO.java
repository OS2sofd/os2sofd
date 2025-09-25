package dk.digitalidentity.sofd.controller.api.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubstituteAssignmentCreateDTO {
	private long substituteContextId;
	private List<String> constraintOrgUnitUuids;
	private String personUuid;
	private String substitutePersonUuid;
	private String userId;
}
