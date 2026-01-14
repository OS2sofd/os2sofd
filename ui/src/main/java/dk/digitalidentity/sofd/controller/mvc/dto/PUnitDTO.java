package dk.digitalidentity.sofd.controller.mvc.dto;

import org.apache.commons.lang3.StringUtils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PUnitDTO {
	
	private String number;
	
	private String street;
	
	private String postalCode;
	
	private String city;
	
	private String name;

	public boolean isValid() {
		return !StringUtils.isBlank(number) && !StringUtils.isBlank(street) && !StringUtils.isBlank(postalCode) && !StringUtils.isBlank(city) && !StringUtils.isBlank(name);
	}
}
