package dk.digitalidentity.sofd.controller.rest.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactDTO {
	private String keywords;
	private String notes;
	private String contactAddress;
	private String openingHours;
	private String openingHoursPhone;
	private String location;
	private String emailNotes;
	private String urlAddress;
	private String email;
}
