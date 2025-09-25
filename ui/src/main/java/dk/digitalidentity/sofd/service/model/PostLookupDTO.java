package dk.digitalidentity.sofd.service.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostLookupDTO {

	@JsonProperty("CVRAdresse_husnummer")
	private String number;
	
	@JsonProperty("CVRAdresse_postdistrikt")
	private String city;
	
	@JsonProperty("CVRAdresse_vejnavn")
	private String street;
	
	@JsonProperty("CVRAdresse_postnummer")
	private String postalCode;
	
}
