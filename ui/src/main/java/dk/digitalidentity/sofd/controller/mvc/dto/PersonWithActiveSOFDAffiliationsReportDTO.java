package dk.digitalidentity.sofd.controller.mvc.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PersonWithActiveSOFDAffiliationsReportDTO {
	private String uuid;
	private String name;
	private String userId;
	private String affiliationName;
	private String affiliationOrgUnitName;
}
