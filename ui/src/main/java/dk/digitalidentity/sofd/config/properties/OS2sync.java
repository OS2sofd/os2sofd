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
	
	@JsonIgnore
	private String datasourceUrl;
	
	@JsonIgnore
	private String datasourceUsername;
	
	@JsonIgnore
	private String datasourcePassword;

	@JsonIgnore
	private String restUrl;
}
