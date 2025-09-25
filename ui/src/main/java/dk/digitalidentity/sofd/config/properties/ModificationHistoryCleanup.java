package dk.digitalidentity.sofd.config.properties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModificationHistoryCleanup {
	private int days = 10;
}
