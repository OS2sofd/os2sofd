package dk.digitalidentity.sofd.controller.mvc.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class PersonWithActiveSOFDAffiliationsReportDTO {
	private String uuid;
	private String name;
	private String userId;
	private String affiliationName;
	private String affiliationOrgUnitName;
	private Date startDate;
	private Date stopDate;
}
