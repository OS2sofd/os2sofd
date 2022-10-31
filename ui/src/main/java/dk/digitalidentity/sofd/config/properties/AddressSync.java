package dk.digitalidentity.sofd.config.properties;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AddressSync {
    @FeatureDocumentation(name = "Navne- og adressesynkronisering", description = "Automatisk opdatering af navn og adresse fra CPR")
    private boolean enabled;
    private boolean runOnStartup;
}
