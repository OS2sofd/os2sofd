package dk.digitalidentity.sofd.controller.mvc.dto;

import dk.digitalidentity.sofd.dao.model.enums.AffiliationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AffiliationDTO {
	private String uuid;
	private AffiliationType affiliationType;
	private boolean inheritPrivilegesFromOU;
	private String orgUnitUuid; 
	private String orgUnitName;
	private String fromOrgUnitUuid;
	private String fromOrgUnitName;
	private String positionName;
	private String startDate;
	private String stopDate;
	private String vendor;
	private String internalReference;
	private boolean useAsPrimaryWhenActive;
}
