package dk.digitalidentity.sofd.config.properties;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AffiliationWorkplace {

	@FeatureDocumentation(name = "Flere arbejdssteder på tilhørsforhold", description = "Nogle typer ansættelser (f.eks. elever) rykker nogle gange rundt mellem enheder, selvom de kun har et tilhørsforhold. Hvis denne feature er slået til, kan man oprette arbejdssteder, og dermed tilknytte flere enheder på et tilhørsforhold.")
	private boolean enabled = false;

}
