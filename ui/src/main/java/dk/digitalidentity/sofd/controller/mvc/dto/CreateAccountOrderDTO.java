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
public class CreateAccountOrderDTO {
	private String personUuid;
	private String personName;
	private String userType;
	private String affiliationUuid;
	private String userId;
	private String chosenUserId;
	private EndDate userEndDate;
	private boolean showEndDate;
}
