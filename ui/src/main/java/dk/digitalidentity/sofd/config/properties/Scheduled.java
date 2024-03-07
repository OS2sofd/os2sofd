package dk.digitalidentity.sofd.config.properties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Scheduled {
	private ModificationHistoryCleanup modificationHistoryCleanup = new ModificationHistoryCleanup();
	private AccountOrderGeneration accountOrderGeneration = new AccountOrderGeneration();
	private ManagerSync managerSync = new ManagerSync();
	private FlagCleanup flagCleanup = new FlagCleanup();
	private CprSync cprSync = new CprSync();
	private DeleteOldAffiliations deleteOldAffiliations = new DeleteOldAffiliations();

	private boolean enabled;
	private boolean generateAccountOrdersOnStartupEnabled = false;
}
