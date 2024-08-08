package dk.digitalidentity.sofd.config.properties;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Los {
	
	@FeatureDocumentation(name = "Vedligehold organisation", description = "Mulighed for at vedligeholde organisation")
	private boolean enabled = false;

	@FeatureDocumentation(name = "Alternative organisationer", description = "Mulighed for oprettelse af flere organisationer")
	private boolean altOrgsEnabled = false;
	
	@FeatureDocumentation(name = "Fremtidig organisation", description = "Mulighed for at planlægge organisationsændringer i fremtiden")
	private boolean futureOrgsEnabled = false;
	
	private String primeAffiliationMaster = "OPUS";
}
