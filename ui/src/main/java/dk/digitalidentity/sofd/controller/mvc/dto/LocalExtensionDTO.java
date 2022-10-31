package dk.digitalidentity.sofd.controller.mvc.dto;

import dk.digitalidentity.sofd.controller.mvc.dto.enums.LocalExtensionType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocalExtensionDTO {
	private LocalExtensionType type;
	private String object;
	private String field;
	private String value;
}
