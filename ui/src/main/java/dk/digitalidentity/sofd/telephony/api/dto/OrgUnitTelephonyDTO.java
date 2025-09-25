package dk.digitalidentity.sofd.telephony.api.dto;

import dk.digitalidentity.sofd.telephony.dao.model.TelephonyPhoneOrgUnit;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrgUnitTelephonyDTO {
	private String orgunitUuid;
	private String orgunitName;

	public OrgUnitTelephonyDTO(TelephonyPhoneOrgUnit ou) {
		this.orgunitName = ou.getOrgunitName();
		this.orgunitUuid = ou.getOrgunitUuid();
	}
}
