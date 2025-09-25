package dk.digitalidentity.sofd.config.properties;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Chart {
	
	@FeatureDocumentation(name = "Organisationsdiagrammer", description = "Gør det muligt at lave og vise organisationsdiagrammer")
	private boolean enabled = false;
}
