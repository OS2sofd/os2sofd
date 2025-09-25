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
	private boolean linkSubstituteADAccountsToExchange = false;
	private boolean useCprNameForUsernameGenerator = false;
	private String idmCloseExemptedUserRegex;

	// used by Billund to encode cpr numbers during transport
	private boolean encodeCpr = false;

	// manager-approval flow
	private boolean accountOrderApprove = false;
	private boolean customApprovalForm = false;
	// if this is enabled, all managers of the person will be able to approve the order
	// even though the order was triggered by an affiliation that the manager is not manager for
	private boolean allManagersApproveEnabled = false;
	
	// should not be disabled if the municipality wants to user SAME_AS_OTHER for username generation,
	// as the reservation system is required for that
	private boolean reservationEnabled = true;

	// specifies how many days account orders stay in queue after completion
	private int accountOrderRetentionDays = 35;
}
