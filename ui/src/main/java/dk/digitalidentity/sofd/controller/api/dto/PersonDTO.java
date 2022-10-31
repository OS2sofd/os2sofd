package dk.digitalidentity.sofd.controller.api.dto;

import dk.digitalidentity.sofd.dao.model.Person;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonDTO {
	private String uuid;
	private String firstname;
	private String surname;
	private String cpr;
	private String employeeId;
	
	public PersonDTO() {
		
	}

	public PersonDTO(Person person, String employeeId) {
		this.uuid = person.getUuid();
		this.firstname = person.getFirstname();
		this.surname = person.getSurname();
		this.cpr = person.getCpr();
		this.employeeId = employeeId;
	}
}
