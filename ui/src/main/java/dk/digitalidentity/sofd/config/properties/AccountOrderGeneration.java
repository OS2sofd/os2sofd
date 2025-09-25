package dk.digitalidentity.sofd.config.properties;

import java.util.List;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderDeactivateAndDeleteRule;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountOrderGeneration {
	private List<String> masters;
	private boolean ignoreDeleteOrdersIfNoAffiliations = true;
	private AccountOrderDeactivateAndDeleteRule affiliationDeactivateAndDeleteRuleDefault = AccountOrderDeactivateAndDeleteRule.KEEP_ALIVE;
	@FeatureDocumentation(name = "Udførelse af AD tasks", description = "Bruges til at definere den time kontobestillinger skal udføres, i 24 timers format")
	private int timeOfExecution = 9;
}
