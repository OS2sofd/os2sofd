package dk.digitalidentity.sofd.config.properties;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Manager {
	
	@FeatureDocumentation(name = "Lederopmærkning", description = "Mulighed for at angive leder for enheder")
	private boolean editEnabled = false;
}
