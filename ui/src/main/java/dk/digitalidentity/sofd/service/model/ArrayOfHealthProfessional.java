package dk.digitalidentity.sofd.service.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "ArrayOfHealthProfessional", namespace = "http://sst.dk/")
public class ArrayOfHealthProfessional {
    @JacksonXmlProperty(localName = "HealthProfessional")
    private List<HealthProfessional> healthProfessionals;
}
