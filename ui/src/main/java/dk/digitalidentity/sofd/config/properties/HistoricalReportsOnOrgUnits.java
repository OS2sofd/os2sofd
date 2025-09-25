package dk.digitalidentity.sofd.config.properties;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HistoricalReportsOnOrgUnits {
	
	@FeatureDocumentation(name = "Historikrapporter på enheder", description = "Danner historik rapporter for enheder")
	private boolean enabled = false;
}
