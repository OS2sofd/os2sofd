package dk.digitalidentity.sofd.service.model;


import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PUnitLookupDTO {
	
	@JsonProperty("beliggenhedsadresse")
	private PostLookupDTO post;
	
	@JsonProperty("produktionsenhedsnavn")
	private PUnitName pUnitName;
}
