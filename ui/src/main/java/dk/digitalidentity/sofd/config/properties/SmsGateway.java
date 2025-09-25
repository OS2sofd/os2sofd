package dk.digitalidentity.sofd.config.properties;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SmsGateway {
	
	@FeatureDocumentation(name = "Kommunikation", description = "Modul til afsendelse af SMS og notifikationer om password udløb mm.")
	private boolean enabled = false;
	
	@FeatureDocumentation(name = "SMS slået til", description = "Angiver om SMS er slået til")
	private boolean smsEnabled = true;

	@FeatureDocumentation(name = "Password påmindelse", description = "Advisering ved snarligt udløb af AD kodeord")
	private boolean pwdReminderEnabled = false;

	@JsonIgnore
	private String url = "";
}
