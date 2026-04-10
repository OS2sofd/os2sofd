package dk.digitalidentity.sofd.controller.mvc.dto;

import dk.digitalidentity.sofd.dao.model.enums.PersonType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDTO {
	private String personUuid;
	
	private String chosenName;

	private String firstname;

	private String surname;

	private PersonType personType;
}
