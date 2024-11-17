package dk.digitalidentity.sofd.controller.api.dto;

import dk.digitalidentity.sofd.dao.model.Person;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

	public PersonDTO(Person person, String employeeId, boolean encodeCpr) {
		this.uuid = person.getUuid();
		this.firstname = person.getFirstname();
		this.surname = person.getSurname();
		this.cpr = (encodeCpr) ? encodeCpr(person.getCpr()) : person.getCpr();
		this.employeeId = employeeId;
	}
	
    private String encodeCpr(String cpr) {
    	if (cpr == null) {
    		return null;
    	}

    	// special rule - if starts with "0000" is is not encoded, but a fictive cpr value
    	if (cpr.startsWith("0000")) {
    		return cpr;
    	}

    	try {
    		long val = Long.parseLong(cpr);

    		val++;
            val *= 33;

            cpr = Long.toString(val);
        }
    	catch (Exception ex) {
    		log.error("Unable to parse cpr: " + cpr, ex);
    	}

        return cpr;
    }
}
