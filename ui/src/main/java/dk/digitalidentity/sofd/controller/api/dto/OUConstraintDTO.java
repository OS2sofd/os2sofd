package dk.digitalidentity.sofd.controller.api.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class OUConstraintDTO {
	private String uuid;
	private String name;
	private long assignmentId;
}
