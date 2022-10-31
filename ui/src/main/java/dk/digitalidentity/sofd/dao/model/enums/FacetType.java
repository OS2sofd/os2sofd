package dk.digitalidentity.sofd.dao.model.enums;

import lombok.Getter;

@Getter
public enum FacetType {
	FREETEXT("html.enum.facetType.freetext"),
	LIST("html.enum.facetType.list"), 
	ORG("html.enum.facetType.org"),
	EMPLOYEE("html.enum.facetType.employee");
	
	private String message;
	
	private FacetType(String message) {
		this.message = message;
	}
}
