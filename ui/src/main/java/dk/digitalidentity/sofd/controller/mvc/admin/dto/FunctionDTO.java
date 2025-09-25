package dk.digitalidentity.sofd.controller.mvc.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FunctionDTO {

	private long id;
	private String name;
	private String category;
	private String description;
	private String facetIds;
}
