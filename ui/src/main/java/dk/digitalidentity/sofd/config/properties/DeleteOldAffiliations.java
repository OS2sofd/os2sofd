package dk.digitalidentity.sofd.config.properties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeleteOldAffiliations {
	private boolean enabled = false;
	private int days = 90;
}
