package dk.digitalidentity.sofd.controller.mvc.dto;

import java.util.List;
import java.util.stream.Collectors;

import dk.digitalidentity.sofd.dao.model.ContactPlace;
import dk.digitalidentity.sofd.dao.model.Kle;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.mapping.ContactPlaceKleMapping;
import dk.digitalidentity.sofd.dao.model.mapping.ContactPlaceOrgUnitMapping;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContactPlaceDTO {
	private long id;
	private String name;
	private List<Kle> tasks;
	private List<String> users;

	public ContactPlaceDTO(ContactPlace contactPlace) {
		this.id = contactPlace.getId();
		this.name = contactPlace.getContactPlace().getName();
		this.tasks = contactPlace.getTasks().stream().map(ContactPlaceKleMapping::getKle).collect(Collectors.toList());
		this.users = contactPlace.getUsers().stream().filter(m -> !m.isDeleted()).map(ContactPlaceOrgUnitMapping::getOrgUnit).map(OrgUnit::getName).collect(Collectors.toList());
	}
}
