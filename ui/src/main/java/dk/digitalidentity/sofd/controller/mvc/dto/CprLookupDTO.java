package dk.digitalidentity.sofd.controller.mvc.dto;

import java.util.List;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CprLookupDTO {
	private String firstname;
	private String lastname;
	private String street;
	private String localname;
	private String postalCode;
	private String city;
	private String country;
	private boolean addressProtected;
	@JsonProperty(value = "isDead")
	private boolean dead;
	private boolean disenfranchised;
	private List<CprLookupChildDTO> children;

	public void removeAddressInfo() {
		street = null;
		localname = null;
		postalCode = null;
		city = null;
		country = null;
		addressProtected = false;
	}

	public boolean hasAddress() {
		var result = StringUtils.hasLength(street);
		result &= StringUtils.hasLength(postalCode);
		result &= StringUtils.hasLength(city);
		result &= StringUtils.hasLength(country);
		return result;
	}
}
