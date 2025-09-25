package dk.digitalidentity.sofd.config.properties;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CprSync {
    
	@FeatureDocumentation(name = "Synkronisering af børn", description = "Automatisk synkronisering af medarbejderes børn fra CPR")
    private boolean enabledChildren = false;
    
    @FeatureDocumentation(name = "Cpr opdatering", description = "Automatisk opdatering af navn, adresse og civilstand fra CPR")
    private boolean enabled = true;

    private boolean runOnStartup;
}