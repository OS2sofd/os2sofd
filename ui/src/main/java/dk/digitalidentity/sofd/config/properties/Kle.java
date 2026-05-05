package dk.digitalidentity.sofd.config.properties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Kle {
	private boolean enabled = true;
	private String url = "http://klassifikation.digital-identity.dk";
}
