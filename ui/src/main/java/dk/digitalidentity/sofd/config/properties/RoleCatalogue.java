package dk.digitalidentity.sofd.config.properties;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class RoleCatalogue {
	private String url = "";
	
	@JsonIgnore
	private String apiKey = "";
	
	@FeatureDocumentation(name = "OS2rollekatalog (deaktive AD konti)", description = "Hvis denne feature er slået til, overføres disablede AD konti til rollekataloget, så man kan administrere rettigheder på disse")
	private boolean includeDisabled = false;
	
	// how many days before an affiliation becomes active, will we expose it to the role catalogue
	private long affiliationCutoff = 60;
	
	// if set, use payGradeText from OPUS when available, instead of positionName
	private boolean usePayGradeText = false;
}
