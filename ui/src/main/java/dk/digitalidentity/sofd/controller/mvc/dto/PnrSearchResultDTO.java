package dk.digitalidentity.sofd.controller.mvc.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class PnrSearchResultDTO {
	private String pnr;
	private String pnrName;

}
