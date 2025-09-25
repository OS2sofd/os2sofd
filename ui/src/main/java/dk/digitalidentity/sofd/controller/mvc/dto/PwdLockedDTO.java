package dk.digitalidentity.sofd.controller.mvc.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PwdLockedDTO {
	private String smsTxt;
	private boolean enabled;
}
