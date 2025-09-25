package dk.digitalidentity.sofd.controller.api.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginContextResultDTO {
	private String name;
	private String username;
	private String email;
	private List<LoginContextDTO> contexts;
}
