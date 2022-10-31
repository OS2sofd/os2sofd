package dk.digitalidentity.sofd.controller.mvc.dto;

import dk.digitalidentity.sofd.dao.model.User;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PrimeUserDTO {
	private String uuid;
	private boolean prime;
	private String userType;

	public PrimeUserDTO(User user) {
		this.uuid = user.getUuid();
		this.prime = user.isPrime();
		this.userType = user.getUserType();
	}
}
