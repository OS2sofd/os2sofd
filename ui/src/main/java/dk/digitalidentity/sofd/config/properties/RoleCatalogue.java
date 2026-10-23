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

	// same as includeDisabled, but only for the dataset that requires an active or future affiliation, so
	// pre-hire accounts (created disabled ahead of their start date) are exposed, while people who have
	// stopped are not - they are only reachable through the allAD dataset, which this flag does not touch.
	// a disabled account is skipped if an enabled account of the same type is mapped to the same affiliation,
	// as that is an old, replaced account rather than a pre-hire account
	@FeatureDocumentation(name = "OS2rollekatalog (deaktive AD konti med ansættelse)", description = "Hvis denne feature er slået til, overføres disablede AD konti til rollekataloget, hvis personen har en aktiv eller fremtidig ansættelse, og der ikke findes en aktiv AD konto på samme tilhørsforhold. Fratrådte overføres ikke")
	private boolean includeDisabledWithAffiliation = false;
	
	// how many days before an affiliation becomes active, will we expose it to the role catalogue
	private long affiliationCutoff = 60;
	
	// if set, use payGradeText from OPUS when available, instead of positionName
	private boolean usePayGradeText = false;
}
