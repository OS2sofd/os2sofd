package dk.digitalidentity.sofd.controller.api.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmployeeMappingDto {
	private String userId;
	private String employeeId;
	private String futureEmployeeId;
	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate futureDate;
}
