package dk.digitalidentity.sofd.config.properties;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SmsGateway {
	@FeatureDocumentation(name = "Kommunikation", description = "Modul til afsendelse af SMS")
	private boolean enabled = false;
	@FeatureDocumentation(name = "Password påmindelse", description = "SMS advisering ved snarligt udløb af AD kodeord")
	private boolean pwdReminderEnabled = false;
	
	@JsonIgnore
	private String url = "";
}
