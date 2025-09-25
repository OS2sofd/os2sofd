package dk.digitalidentity.sofd.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PnrLookupDTO {
	@JsonProperty("pNummer")
	private String pnr;
	
	@JsonProperty("produktionsenhedsnavn")
	private PUnitName pUnitName;
}
