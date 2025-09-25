package dk.digitalidentity.sofd.controller.mvc.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EmailDTO {
	private long id; // used by the HTML page
	private String name;
	private String email;

	public EmailDTO(String name, String email) {
		this.name = name;
		this.email = email;
	}
}
