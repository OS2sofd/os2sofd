package dk.digitalidentity.sofd.config.properties;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountOrderGeneration {
	private List<String> masters;
	private boolean ignoreDeleteOrdersIfNoAffiliations = true;
}
