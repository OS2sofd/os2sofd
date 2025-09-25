package dk.digitalidentity.sofd.config.properties;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Scheduled {
	private ModificationHistoryCleanup modificationHistoryCleanup = new ModificationHistoryCleanup();
	private AccountOrderGeneration accountOrderGeneration = new AccountOrderGeneration();
	private FlagCleanup flagCleanup = new FlagCleanup();
	private CprSync cprSync = new CprSync();
	private DeleteOldAffiliations deleteOldAffiliations = new DeleteOldAffiliations();
	private DeleteSubstitutes deleteSubstitutes = new DeleteSubstitutes();
	
	private boolean enabled;
	@FeatureDocumentation(name = "Post om natten", description = "Gør at OS2sofd sender postbeskeder hele døgnet, hvor den som default ikke sender post mellem kl. 22 og kl. 05:30")
	private boolean mailDuringNightEnabled = false;
	private boolean generateAccountOrdersOnStartupEnabled = false;
}
