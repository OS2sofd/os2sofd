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
public class Kommunikation {
	
	@JacksonXmlProperty(localName = "Gyldighedsstart")
	private String gyldighedsstart;
	
	@JacksonXmlProperty(localName = "Gyldighedsstop")
	private String gyldighedsstop;
	
	@JacksonXmlProperty(localName = "Sekvensnummer")
	private String sekvensnummer;
	
	@JacksonXmlProperty(localName = "Kommunikationsart")
	private String kommunikationsart;
	
	@JacksonXmlProperty(localName = "KommunikationsID")
	private String kommunikationsID;
}
