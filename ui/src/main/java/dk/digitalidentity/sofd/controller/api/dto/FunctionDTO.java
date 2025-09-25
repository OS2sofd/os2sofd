package dk.digitalidentity.sofd.controller.api.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FunctionDTO {
	private long id;
	private int sortKey;
	private String name;
	private String category;
	private String description;
	private List<FacetDTO> facets;
}
