package dk.digitalidentity.sofd.controller.mvc.admin.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import dk.digitalidentity.sofd.dao.model.enums.FacetType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FacetDTO {

	private long id;
	@Size(min = 1, max = 255, message = "Der skal angives et navn")
	private String name;
	private String description;
	@NotNull(message = "Der skal vælges en type")
	private FacetType type;
	private String pattern;
	private String listItems;
}
