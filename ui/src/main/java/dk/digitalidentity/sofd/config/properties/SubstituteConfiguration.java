package dk.digitalidentity.sofd.config.properties;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubstituteConfiguration {
	
	@FeatureDocumentation(name = "Vikarer", description = "Integration til OS2vikar")
	private boolean enabled = true;
	
	private String masterId = "OS2vikar";
}
