package dk.digitalidentity.sofd.config.properties;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactPlaces {
	
	@FeatureDocumentation(name = "Henvendelsessteder", description = "Registrering af henvendelsessteder")
	private boolean enabled = false;
}