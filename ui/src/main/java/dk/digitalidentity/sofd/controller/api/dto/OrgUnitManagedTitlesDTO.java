package dk.digitalidentity.sofd.controller.api.dto;

import java.util.List;
import java.util.stream.Collectors;

import dk.digitalidentity.sofd.dao.model.OrgUnit;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrgUnitManagedTitlesDTO {
	private String orgUnitUuid;
	private List<ManagedTitleDTO> titles;
	
	public OrgUnitManagedTitlesDTO (OrgUnit orgUnit) {
		this.orgUnitUuid = orgUnit.getUuid();
		this.titles = orgUnit.getManagedTitles().stream().map(m -> new ManagedTitleDTO(m)).collect(Collectors.toList());
	}
}
