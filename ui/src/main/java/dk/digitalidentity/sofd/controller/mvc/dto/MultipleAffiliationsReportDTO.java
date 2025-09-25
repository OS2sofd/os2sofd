package dk.digitalidentity.sofd.controller.mvc.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MultipleAffiliationsReportDTO {
	private String uuid;
	private String name;
	private String cpr;
	private String affiliationName;
	private String affiliationOrgUnitName;
	private boolean primeAffiliation;
	private String affiliationTerms;
	private String employeeId;
	private String userId;
}
