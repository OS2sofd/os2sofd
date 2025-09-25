package dk.digitalidentity.sofd.controller.mvc.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthorizationCodeDTO {
	private long id;
	private String code;
	private String name;
	private boolean prime;
}
