package dk.digitalidentity.sofd.config.properties;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Telephony {
	
	@FeatureDocumentation(name = "Telefoni", description = "Modul til håndtering af telefonnumre")
	private boolean enabled = false;
}
