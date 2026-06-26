package dk.digitalidentity.sofd.controller.api.dto;

import java.util.Date;

import dk.digitalidentity.sofd.dao.model.enums.AccountOrderType;
import dk.digitalidentity.sofd.dao.model.enums.EndDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAccountOrderDTO {
	private String personUuid;
	private String personName;
	private String userType;
	private String affiliationUuid;
	private String userId;
	private String chosenUserId;
	private EndDate userEndDate;
	private boolean showEndDate;
	private Date activationDate;
	private AccountOrderType orderType = AccountOrderType.CREATE;
}
