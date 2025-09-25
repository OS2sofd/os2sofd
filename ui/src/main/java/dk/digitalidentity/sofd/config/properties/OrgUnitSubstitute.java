package dk.digitalidentity.sofd.config.properties;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrgUnitSubstitute {
	
	@FeatureDocumentation(name = "Enhedsstedfortræder", description = "Mulighed for at angive en stedfortræder for enheder")
	private boolean enabled = false;
}
