package dk.digitalidentity.sofd.config.properties;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EBoks {
	@FeatureDocumentation(name = "EBoks", description = "Giver sofd mulighed for at sende EBoks beskeder")
	private boolean enabled = false;
	
	// TODO: old settings, not needed in new setup
	private String materialeId;
	private String senderId;

	// TODO: remove this once everyone is moved
	private boolean newUrlEnabled = false;
	
	// TODO: new settings
	private String senderName;

	@JsonIgnore
	private String oldUrl = "http://remoteprint.digital-identity.dk/api/remotePrint/SendLetterToCpr";
	
	@JsonIgnore
	private String url = "http://sf1601.digital-identity.dk/api/print";
}
