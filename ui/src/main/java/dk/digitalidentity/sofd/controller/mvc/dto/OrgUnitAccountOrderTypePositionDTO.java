package dk.digitalidentity.sofd.controller.mvc.dto;

import dk.digitalidentity.sofd.dao.model.OrgUnitAccountOrderTypePosition;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderRule;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrgUnitAccountOrderTypePositionDTO {
	private String positionName;
	private AccountOrderRule rule;
	private boolean requiresApproval;
	
	public OrgUnitAccountOrderTypePositionDTO(OrgUnitAccountOrderTypePosition orig) {
		this.positionName = orig.getPositionName();
		this.rule = orig.getRule();
		this.requiresApproval = orig.isRequiresApproval();
	}
}
