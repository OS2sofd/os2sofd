package dk.digitalidentity.sofd.controller.api.dto;

import dk.digitalidentity.sofd.dao.model.enums.AccountOrderStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SetOrderStatusDTO {
	private long id;
	private AccountOrderStatus status;
	private String affectedUserId;
	private String message;
}
