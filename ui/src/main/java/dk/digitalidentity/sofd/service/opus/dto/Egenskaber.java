package dk.digitalidentity.sofd.service.opus.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class Egenskaber {

    @JacksonXmlProperty(localName = "Kommunikation")
    @JacksonXmlElementWrapper(useWrapping = false)
	private List<Kommunikation> kommunikation;
}
