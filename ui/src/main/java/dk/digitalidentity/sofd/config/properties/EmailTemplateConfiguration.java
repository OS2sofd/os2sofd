package dk.digitalidentity.sofd.config.properties;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailTemplateConfiguration {
	
	@FeatureDocumentation(name = "Domænefilter på mailskabeloner", description = "Giver mulighed for at have et domænefilter på visse mailskabeloner, så beskeder kun sendes til udvalgte domæner")
	private boolean domainFilterEnabled = false;

	@FeatureDocumentation(name = "Enhedsfilter på mailskabeloner", description = "Giver mulighed for at have et enhedsfilter på visse mailskabeloner, så beskeder ikke bliver sendt, hvis de vedrører udvalgte enheder")
	private boolean orgFilterEnabled = false;
	
	@FeatureDocumentation(name = "AD-filter på mailskabeloner", description = "Giver mulighed for at have et AD-filter på visse mailskabeloner, så beskeder kun bliver sendt, hvis der er en aktiv AD-bruger")
	private boolean adUserFilterEnabled = false;
	
	// allows inputting a raw HTML template
	private boolean rawTemplatesSupported = false;
}
