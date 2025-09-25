package dk.digitalidentity.sofd.service.opus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class StandardRetur {
	private long statusKode;
	private String fejlbeskedTekst;
}
