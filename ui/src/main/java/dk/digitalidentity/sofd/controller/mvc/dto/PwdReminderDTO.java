package dk.digitalidentity.sofd.controller.mvc.dto;

import java.util.Set;

import dk.digitalidentity.sofd.service.model.PwdReminderStrategy;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PwdReminderDTO {
	private String smsTxt;
	private String emailTxt;
	private String emailSubject;
	private String days;
	private String time;
	private Set<String> orgUnits;
	private PwdReminderStrategy strategy; 
}
