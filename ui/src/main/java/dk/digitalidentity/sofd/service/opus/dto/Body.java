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
public class Body {
	
	@JacksonXmlProperty(localName = "LaesOutput")
	private LaesOutput laesOutput;
	
	@JacksonXmlProperty(localName = "RetOutput")
	private RetOutput retOutput;
}
