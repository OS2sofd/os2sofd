package dk.digitalidentity.sofd.service.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class HealthProfessional {
	
	@JacksonXmlProperty(localName = "AuthorizationID")
	private String authorizationID;
	
	@JacksonXmlProperty(localName = "AuthorizationValid")
	private boolean authorizationValid;

	@JacksonXmlProperty(localName = "AuthorizationDate")
	private LocalDateTime authorizationDate;

	@JacksonXmlProperty(localName = "ProfessionCodeName")
	private String professionCodeName;
}
