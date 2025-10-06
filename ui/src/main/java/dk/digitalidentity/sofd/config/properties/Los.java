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

	@FeatureDocumentation(name = "Foretrukket primært Ansættelsesforhold", description = "Mulighed for at angive hvilket Ansættelsesforhold (f.eks. 01 Månedsløn Bagud) der prioriteres højest ved vurdering af primær ansættelse. Normalt prioriteres lavest (00) højest.")
	private String preferredPrimeEmploymentTerms;
	
	private boolean allowSettingMasterId = false;

	private boolean editPnrOnAllOrgUnitsEnabled = false;
}
