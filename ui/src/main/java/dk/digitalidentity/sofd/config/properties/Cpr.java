package dk.digitalidentity.sofd.config.properties;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Cpr {
	private String url = "http://cprservice5.digital-identity.dk";
	private boolean useAddressName = false;
	private boolean addressEnabled = false;
	private boolean showPersonStateInUi = false;
}
