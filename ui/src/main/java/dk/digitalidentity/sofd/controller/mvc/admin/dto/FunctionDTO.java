package dk.digitalidentity.sofd.controller.mvc.admin.dto;

import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FunctionDTO {

	private long id;
	@Size(min = 1, max = 255, message = "Der skal angives et navn")
	private String name;
	private String description;
	private String facetIds;
}
