package dk.digitalidentity.sofd.controller.mvc.dto;

import dk.digitalidentity.sofd.dao.model.enums.EndDate;
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
public class CreateDoubleAccountOrderDTO {
	private String personUuid;
	private String personName;
	private String userType;
	private String adAffiliationUuid;
	private String adChosenUserId;
	private EndDate adUserEndDate;
	private String exchangeChosenUserId;
	private boolean requireApproval;
}
