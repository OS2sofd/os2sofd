package dk.digitalidentity.sofd.dao.model.enums;

import lombok.Getter;

@Getter
public enum FacetType {
	FREETEXT("html.enum.facetType.freetext"),
	LIST("html.enum.facetType.list"), 
	ORG("html.enum.facetType.org"),
	EMPLOYEE("html.enum.facetType.employee"),
	FOLLOW_UP_DATE("html.enum.facetType.follow_up_date");
	
	private String message;
	
	private FacetType(String message) {
		this.message = message;
	}
}
