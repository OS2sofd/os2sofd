package dk.digitalidentity.sofd.config.properties;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PersonComments {
	
	@FeatureDocumentation(name = "Kommentarfelt på person", description = "Mulighed for at angive kommentarer på personer")
	private boolean enabled = false;
}
