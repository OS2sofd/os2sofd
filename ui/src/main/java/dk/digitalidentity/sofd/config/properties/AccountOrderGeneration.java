package dk.digitalidentity.sofd.config.properties;

import java.util.ArrayList;
import java.util.List;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderDeactivateAndDeleteRule;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountOrderGeneration {
	private List<String> masters;
	private List<String> organisations = new ArrayList<>(List.of("ADMORG"));
	private boolean ignoreDeleteOrdersIfNoAffiliations = true;
	private AccountOrderDeactivateAndDeleteRule affiliationDeactivateAndDeleteRuleDefault = AccountOrderDeactivateAndDeleteRule.KEEP_ALIVE;
	@FeatureDocumentation(name = "Udførelse af AD tasks", description = "Bruges til at definere den time kontobestillinger skal udføres, i 24 timers format")
	private int timeOfExecution = 9;

	// failsafe thresholds: if pending orders of a given type exceed the threshold, the API will refuse to return them
	private int pendingOrderCreateThreshold = Integer.MAX_VALUE;
	private int pendingOrderDeactivateThreshold = Integer.MAX_VALUE;
	private int pendingOrderDeleteThreshold = Integer.MAX_VALUE;
	private int pendingOrderExpireThreshold = Integer.MAX_VALUE;
}
