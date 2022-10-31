package dk.digitalidentity.sofd.telephony.controller.rest.dto;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import dk.digitalidentity.sofd.telephony.dao.model.TelephonyHistoryView;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TelephonyHistoryDTO {
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private String phoneNumber;
	private String person;
	private LocalDate assignedFrom;
	private LocalDate assignedTo;

	public TelephonyHistoryDTO(TelephonyHistoryView t) {
		this.phoneNumber = t.getPhoneNumber();
		this.person = t.getName();
		this.assignedFrom = LocalDate.parse(t.getTimestamp(), formatter);
	}
	
	public String getAssignedFromAsString() {
		return formatter.format(assignedFrom);
	}
	
	public String getAssignedToAsString() {
		if (assignedTo != null) {
			return formatter.format(assignedTo);
		}
		
		return "";
	}
}
