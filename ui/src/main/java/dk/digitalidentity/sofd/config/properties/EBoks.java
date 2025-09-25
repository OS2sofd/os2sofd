package dk.digitalidentity.sofd.config.properties;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class EBoks {
	
	@FeatureDocumentation(name = "Digital Post", description = "Giver sofd mulighed for at sende Digital Post beskeder")
	private boolean enabled = false;
	
	private String senderName;
	
	@JsonIgnore
	private String url = "http://sf1601.digital-identity.dk/api/print";

    private int additionalDelayMinutes = 0;
}
