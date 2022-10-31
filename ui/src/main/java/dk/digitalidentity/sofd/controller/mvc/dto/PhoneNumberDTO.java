package dk.digitalidentity.sofd.controller.mvc.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PhoneNumberDTO {
	private long id; // used by the HTML page
	private String name;
	private String phone;
	private RecipientType type;
	
	public PhoneNumberDTO(String name, String phone, RecipientType type) {
		this.name = name;
		this.phone = phone;
		this.type = type;
	}
}
