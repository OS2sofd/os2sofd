package dk.digitalidentity.sofd.controller.api.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountOrderResponseDTO {
	private boolean singleAccount;

	private List<AccountOrderDTO> pendingOrders;
}
