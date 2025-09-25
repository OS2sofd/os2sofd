package dk.digitalidentity.sofd.config.properties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Profile {
	private boolean enabled;
	
	public boolean isEnabled() {
		return enabled;
	}
}
