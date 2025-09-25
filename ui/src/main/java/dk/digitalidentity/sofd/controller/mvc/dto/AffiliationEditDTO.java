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
public class AffiliationEditDTO {
	private String uuid;
	private String positionName;
	private String startDate;
	private String stopDate;
	private String personUuid;
	private String vendor;
	private String internalReference;
	private AffiliationType affiliationType;
	private String positionDisplayName;
	private boolean doNotTransferToFKOrg;
}
