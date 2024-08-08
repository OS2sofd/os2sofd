package dk.digitalidentity.sofd.config.properties;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FunctionHierarchy {
	
	@FeatureDocumentation(name = "Funktionshierarki", description = "Gør det muligt at vedligeholde funktioner og funktionsfacetter")
	private boolean enabled = false;
	private  String displayUserType = "ACTIVE_DIRECTORY";
}
