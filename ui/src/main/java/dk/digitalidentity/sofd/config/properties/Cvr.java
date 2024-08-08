package dk.digitalidentity.sofd.config.properties;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Cvr {
	
	@FeatureDocumentation(name = "Cvr", description = "Giver sofd mulighed for at hente cvr data")
	private boolean enabled = false;

	private String apiKey = "";
	private String baseUrl = "https://datafordeler.digital-identity.dk/proxy";
}
