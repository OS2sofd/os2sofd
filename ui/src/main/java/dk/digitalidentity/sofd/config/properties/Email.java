package dk.digitalidentity.sofd.config.properties;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Email {
	@FeatureDocumentation(name = "Email", description = "Giver sofd mulighed for at afsende emails")
	private boolean enabled = false;
	private String from = "no-reply@sofd.io";
	private String fromName = "SOFD Core";

	@JsonIgnore
	private String username;
	
	@JsonIgnore
	private String password;

	@JsonIgnore
	private String host;
}
