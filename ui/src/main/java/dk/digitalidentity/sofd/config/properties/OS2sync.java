package dk.digitalidentity.sofd.config.properties;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OS2sync {
	
	@FeatureDocumentation(name = "OS2sync", description = "Synkronisering til FK Organisation")
	private boolean enabled = false;
	private boolean cprEnabled = false;
	
	// only ever enable one of these - the STIL need should be phased out, and the school one should be enabled by default at some point
	// it looks at the UserType field to decide what user account to use for schools
	private boolean stilEnabled = false;
	private boolean schoolEnabled = false;

	private boolean useObjectGuidAsKombitUuid = true;
	private boolean doNotSendCprForSubstitutes = true;
	
	// default - can be overriden in some municipalities
	private String substituteRegex = "^vik\\d+$";
	
	@JsonIgnore
	private String datasourceUrl;
	
	@JsonIgnore
	private String datasourceUsername;
	
	@JsonIgnore
	private String datasourcePassword;

	@JsonIgnore
	private String restUrl;
	
	@JsonIgnore
	private String userCleanupUrl;

	private boolean sendHiddenPhoneNumbers = true;
}
