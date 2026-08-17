package dk.digitalidentity.sofd.config.properties;

import java.util.HashSet;
import java.util.Set;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderDeactivateAndDeleteRule;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountOrderGeneration {
	// any affiliation where master is in this set, is used for triggereing IdM processes 
	private Set<String> masters;
	
	// optional list - if empty just use the above "masters" for AD, but if filled out, use this set instead for AD accounts
	private Set<String> adOptionalMasters;
	
	// optional list - if available, do pre-validation on OPUS accounts before creation to ensure they start with one of these
	private Set<String> opusOptionalPrefix;

	@FeatureDocumentation(name = "Adskilt håndtering af eksterne konti", description = "Når slået til, kan eksterne tilhørsforhold udløse deres egen AD-konto, og en konto holdes kun i live af tilhørsforhold af samme type. Når slået fra, udløser kun medarbejder-tilhørsforhold kontobestillinger, en eksisterende konto blokerer nye, og ethvert aktivt tilhørsforhold holder kontoen i live")
	private boolean separateExternalAccounts = false;

	private Set<String> organisations = new HashSet<>(Set.of("ADMORG"));
	private boolean ignoreDeleteOrdersIfNoAffiliations = true;
	private AccountOrderDeactivateAndDeleteRule affiliationDeactivateAndDeleteRuleDefault = AccountOrderDeactivateAndDeleteRule.KEEP_ALIVE;

	@FeatureDocumentation(name = "Udførelse af AD tasks", description = "Bruges til at definere den time kontobestillinger skal udføres, i 24 timers format")
	private int timeOfExecution = 9;

	// failsafe thresholds: if pending orders of a given type exceed the threshold, the API will refuse to return them
	private int pendingOrderCreateThreshold = Integer.MAX_VALUE;
	private int pendingOrderDeactivateThreshold = Integer.MAX_VALUE;
	private int pendingOrderDeleteThreshold = Integer.MAX_VALUE;
	private int pendingOrderExpireThreshold = Integer.MAX_VALUE;
	private int pendingOrderCleanupThreshold = Integer.MAX_VALUE;
}
