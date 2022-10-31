package dk.digitalidentity.sofd.config.properties;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AccountCreation {
	private OpusAccountOrderHandler opusHandler = new OpusAccountOrderHandler();
	@FeatureDocumentation(name = "Kontooprettelse", description = "Generering af kontooprettelsesordrer")
	private boolean enabled = false;
	private String hourlyWageCode = "03";
	private boolean reuseExistingUsernames = false;
}
