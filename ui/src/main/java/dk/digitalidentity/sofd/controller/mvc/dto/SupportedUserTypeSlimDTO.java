package dk.digitalidentity.sofd.controller.mvc.dto;

import dk.digitalidentity.sofd.dao.model.SupportedUserType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SupportedUserTypeSlimDTO {
	private String key;
	private String name;
	
	public SupportedUserTypeSlimDTO(SupportedUserType userType) {
		this.key = userType.getKey();
		this.name = userType.getName();
	}
}
