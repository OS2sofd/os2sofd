package dk.digitalidentity.sofd.config.properties;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Manager {
	
	@FeatureDocumentation(name = "Lederopmærkning", description = "Mulighed for at angive leder for enheder")
	private boolean editEnabled = false;

	@FeatureDocumentation(name = "Ryd lederopmærkning ved ændring", description = "Rydder en manuelt angivet leder i OS2sofd såfremt lederangivelsen fra kildesystemet ændres")
	private boolean clearSelectedManagerOnSourceManagerChange = false;

}
