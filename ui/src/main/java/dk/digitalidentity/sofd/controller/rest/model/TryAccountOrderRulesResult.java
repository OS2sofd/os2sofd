package dk.digitalidentity.sofd.controller.rest.model;

import java.util.Map;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TryAccountOrderRulesResult {
	private Map<String, Long> result;
}
