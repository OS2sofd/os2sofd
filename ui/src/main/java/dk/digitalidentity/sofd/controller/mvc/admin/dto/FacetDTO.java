package dk.digitalidentity.sofd.controller.mvc.admin.dto;

import dk.digitalidentity.sofd.dao.model.enums.FacetType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FacetDTO {

	private long id;
	private String name;
	private String description;
	private FacetType type;
	private String pattern;
	private String listItems;
}
