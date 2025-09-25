package dk.digitalidentity.sofd.controller.api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginContextDTO {
	private String uuid;
	private String name;
	private LoginContextRole role;
	private String ouUuids;
}
