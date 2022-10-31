package dk.digitalidentity.sofd.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PUnitName {
	
	@JsonProperty("vaerdi")
	private String value;

}
