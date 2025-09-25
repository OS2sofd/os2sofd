package dk.digitalidentity.sofd.controller.mvc.dto;

import java.util.List;

import dk.digitalidentity.sofd.controller.mvc.dto.enums.SMSChannel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SearchForm {
	private List<String> itSystems;
	private List<String> orgUnits;
	private String operation;
	private String function = "NONE";
	private boolean includeOrgUnits;
	private String type;
	private SMSChannel channel;
}
