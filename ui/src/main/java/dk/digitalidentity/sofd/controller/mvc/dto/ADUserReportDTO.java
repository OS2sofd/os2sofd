package dk.digitalidentity.sofd.controller.mvc.dto;

import java.time.LocalDate;

import dk.digitalidentity.sofd.controller.mvc.dto.enums.ADUserStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ADUserReportDTO {
	private String personUuid;
	private String name;
	private String userId;
	private ADUserStatus status;
	private LocalDate created;
	private LocalDate closed;
}
