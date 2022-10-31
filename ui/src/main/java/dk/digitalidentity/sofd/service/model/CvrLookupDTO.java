package dk.digitalidentity.sofd.service.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CvrLookupDTO {
	@JsonProperty("produktionsenheder")
	private List<PnrLookupDTO> pnrs;
}
