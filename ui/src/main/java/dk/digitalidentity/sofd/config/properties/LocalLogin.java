package dk.digitalidentity.sofd.config.properties;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocalLogin {
	
	@FeatureDocumentation(name = "Lokalt login", description = "Mulighed for at logge på SOFD med brugernavn/password uden om IDP")
	private boolean enabled = false;
}
