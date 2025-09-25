package dk.digitalidentity.sofd.controller.mvc.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
@Setter
public class PersonWithAffiliationsWorkplacesReportDTO {
	private String uuid;
	private String name;
	private String affiliationUuid;
	private String employeeNumber;
	private String positionName;
	private String affiliationStartDate;
	private String affiliationStopDate;
	private String affiliationPlacement;
	private String workplace;
	private String workplaceStartDate;
	private String workplaceStopDate;
}
