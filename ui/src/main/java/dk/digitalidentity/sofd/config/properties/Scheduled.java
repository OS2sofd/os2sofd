package dk.digitalidentity.sofd.config.properties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Scheduled {
	private ModificationHistoryCleanup modificationHistoryCleanup = new ModificationHistoryCleanup();
	private AddressSync addressSync = new AddressSync();
	private ChildrenSync childrenSync = new ChildrenSync();
	private AccountOrderGeneration accountOrderGeneration = new AccountOrderGeneration();
	private ManagerSync managerSync = new ManagerSync();
	private FlagCleanup flagCleanup = new FlagCleanup();

	private boolean enabled;
}
