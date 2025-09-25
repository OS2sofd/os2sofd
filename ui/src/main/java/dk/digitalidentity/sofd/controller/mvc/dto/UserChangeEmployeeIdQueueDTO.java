package dk.digitalidentity.sofd.controller.mvc.dto;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserChangeEmployeeIdQueueDTO {
	private LocalDate date;
	private String employeeId;
}
