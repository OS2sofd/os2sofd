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
	private boolean forceSetEmployeeId = false;
	private boolean accountOrderApprove = false;
	private boolean linkSubstituteADAccountsToExchange = false;
	private boolean useCprNameForUsernameGenerator = false;

	// should not be disabled if the municipality wants to user SAME_AS_OTHER for username generation,
	// as the reservation system is required for that
	private boolean reservationEnabled = true;
}
