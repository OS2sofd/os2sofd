package dk.digitalidentity.sofd.controller.rest.admin.model;

import dk.digitalidentity.sofd.dao.model.Profession;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ProfessionDto {
	private Long id;
	private Long organisationId;
	private String name;
	private List<ProfessionMappingDto> mappings;

	public ProfessionDto(Profession profession) {
		this.id = profession.getId();
		this.organisationId = profession.getOrganisationId();
		this.name = profession.getName();
		this.mappings = profession.getProfessionMappings().stream().map(ProfessionMappingDto::new).toList();
	}

}
