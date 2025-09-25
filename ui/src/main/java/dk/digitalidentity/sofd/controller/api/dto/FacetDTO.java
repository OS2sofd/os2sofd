package dk.digitalidentity.sofd.controller.api.dto;

import java.util.List;

import dk.digitalidentity.sofd.dao.model.enums.FacetType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FacetDTO {
	private long id;
	private String name;
	private String description;
	private FacetType type;
	private String pattern;
	private List<String> listItems;
	private long sortKey;
}
