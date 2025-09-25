package dk.digitalidentity.sofd.service.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ADGridAffiliation {
	private String positionName;
	private String professionName;
	private String orgUnitUuid;
	private boolean doNotInherit;
}
