package dk.digitalidentity.sofd.service.opus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class LaesResultat {
	
	@JacksonXmlProperty(localName = "Registrering")
	private Registrering registrering;
}
