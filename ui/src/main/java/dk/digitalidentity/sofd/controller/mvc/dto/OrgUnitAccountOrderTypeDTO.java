package dk.digitalidentity.sofd.controller.mvc.dto;

import java.util.ArrayList;
import java.util.List;

import dk.digitalidentity.sofd.dao.model.OrgUnitAccountOrderType;
import dk.digitalidentity.sofd.dao.model.OrgUnitAccountOrderTypePosition;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderDeactivateAndDeleteRule;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderRule;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrgUnitAccountOrderTypeDTO {
	private String userType;
	private long daysBeforeToCreate;
	private long daysBeforeToReactivate;
	private AccountOrderRule rule;
	private boolean requiresApproval;
	private AccountOrderDeactivateAndDeleteRule deactivateAndDeleteRule;
	private List<OrgUnitAccountOrderTypePositionDTO> positions;
	private boolean localRules;

	// settings copied from SupportedUserTypes
	private long globalDaysBeforeToReactivate;
	private long globalDaysBeforeToCreate;
	private boolean createEnabled;
	private boolean reactivateEnabled;

	public OrgUnitAccountOrderTypeDTO(OrgUnitAccountOrderType orig) {
		this.userType = orig.getUserType();
		this.localRules = orig.isLocalRules();
		this.daysBeforeToCreate = orig.getDaysBeforeToCreate();
		this.daysBeforeToReactivate = orig.getDaysBeforeToReactivate();
		this.rule = orig.getRule();
		this.requiresApproval = orig.isRequiresApproval();
		this.deactivateAndDeleteRule = orig.getDeactivateAndDeleteRule();
		this.positions = new ArrayList<OrgUnitAccountOrderTypePositionDTO>();
		
		if (orig.getPositions() != null) {
			for (OrgUnitAccountOrderTypePosition origPosition : orig.getPositions()) {
				this.positions.add(new OrgUnitAccountOrderTypePositionDTO(origPosition));
			}
		}
	}
}
