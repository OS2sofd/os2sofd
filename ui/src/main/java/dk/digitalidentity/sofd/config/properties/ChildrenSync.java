package dk.digitalidentity.sofd.config.properties;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ChildrenSync {
    @FeatureDocumentation(name = "Synkronisering af børn", description = "Automatisk synkronisering af medarbejderes børn fra CPR")
    private boolean enabled = false;
    private boolean runOnStartup = false;
}