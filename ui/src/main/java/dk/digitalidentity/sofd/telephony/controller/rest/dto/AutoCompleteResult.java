package dk.digitalidentity.sofd.telephony.controller.rest.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AutoCompleteResult {
	private List<ValueData> suggestions = new ArrayList<ValueData>();
}
