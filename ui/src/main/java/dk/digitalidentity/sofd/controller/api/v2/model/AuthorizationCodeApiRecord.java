package dk.digitalidentity.sofd.controller.api.v2.model;

import javax.validation.constraints.NotNull;

import dk.digitalidentity.sofd.dao.model.AuthorizationCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AuthorizationCodeApiRecord extends BaseRecord {

	@NotNull
	private String code;

	@NotNull
	private String name;

	@NotNull
	private boolean prime;

	public AuthorizationCodeApiRecord(AuthorizationCode authorizationCode) {
		this.code = authorizationCode.getCode();
		this.name = authorizationCode.getName();
		this.prime = authorizationCode.isPrime();
	}

	public AuthorizationCode toAuthorizationCode() {
		AuthorizationCode authorizationCode = new AuthorizationCode();
		authorizationCode.setCode(code);
		authorizationCode.setName(name);
		authorizationCode.setPrime(prime);

		return authorizationCode;
	}
}
