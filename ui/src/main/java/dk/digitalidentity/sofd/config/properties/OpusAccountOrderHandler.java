package dk.digitalidentity.sofd.config.properties;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dk.digitalidentity.sofd.config.FeatureDocumentation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OpusAccountOrderHandler {
	
	@FeatureDocumentation(name = "OPUS brugeroprettelse", description = "Oprettelse af OPUS it-bruger på intotype 105")
	private boolean enabled = false;
	
	@FeatureDocumentation(name = "OPUS Email i versaler", description = "Email adressen der registreres i infotype 105 sendes med VERSALER")
	private boolean emailInUpperCase = false;

	private String defaultEmail = null;
	private String defaultEmailDomain = null;
	private String municipalityNumber;
	private boolean overwriteExistingEmails = true;
	
	@JsonIgnore
	private String keystore;
	
	@JsonIgnore
	private String keystorePwd;

	// url contains a parameter that needs to be merged with the municipalityNumber
	private String wsUrl = "https://kmdpiprd.kmd.dk/XISOAPAdapter/MessageServlet?senderParty=&senderService=LPE_Employee_KOM_{CODE}&receiverParty=&receiverService=&interface=Employee_Provide_Out&interfaceNamespace=urn:kmd.dk:LPE:EMPLOYEE:external";

	@JsonIgnore
	private String convertedUrl = null;
	
	@JsonIgnore
	public String getConvertedUrl() {
		if (convertedUrl != null) {
			return convertedUrl;
		}
		
		convertedUrl = wsUrl.replace("{CODE}", municipalityNumber);
		
		return convertedUrl;
	}
}
