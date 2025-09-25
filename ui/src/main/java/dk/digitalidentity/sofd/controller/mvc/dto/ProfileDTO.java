package dk.digitalidentity.sofd.controller.mvc.dto;

import javax.validation.constraints.Size;

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
	
	@Size(min = 1)
	private String chosenName;

	private String firstname;

	private String surname;

	private PersonType personType;
}
