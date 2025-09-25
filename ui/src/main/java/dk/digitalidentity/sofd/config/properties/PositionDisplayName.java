package dk.digitalidentity.sofd.config.properties;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PositionDisplayName {
	
    @FeatureDocumentation(name = "Alternativ stillingsbetegnelse", description = "Gør det muligt at lave alternative stillingsbetegnelser")
    private boolean enabled = false;
}
