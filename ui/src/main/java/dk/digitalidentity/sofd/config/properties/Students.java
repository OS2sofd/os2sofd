package dk.digitalidentity.sofd.config.properties;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Students {

	@FeatureDocumentation(name = "Indlæs elever", description = "Indlæs elever fra AD")
	private boolean enabled;
}
