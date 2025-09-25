package dk.digitalidentity.sofd.controller.api.dto;

import dk.digitalidentity.sofd.dao.model.ManagedTitle;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ManagedTitleDTO {
	private String master;
	private String name;
	
	public ManagedTitleDTO(ManagedTitle managedTitle) {
		this.master = managedTitle.getMaster();
		this.name = managedTitle.getName();
	}
}
