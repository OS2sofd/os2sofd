package dk.digitalidentity.sofd.controller.mvc.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SofdAffiliationsReportDTO {
	private String uuid;
	private String name;
	private String cpr;
	private String affiliationName;
	private String affiliationOrgUnitName;
	private String affiliationVendor;
	private String affiliationStartDate;
	private String affiliationStopDate;
}
