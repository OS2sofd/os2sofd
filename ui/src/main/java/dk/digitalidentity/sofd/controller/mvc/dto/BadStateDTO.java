package dk.digitalidentity.sofd.controller.mvc.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BadStateDTO {
	private String cpr;
	private boolean gone;
	private boolean disenfranchised;
	
	@JsonProperty(value = "isDead")
	private boolean dead;	
}
