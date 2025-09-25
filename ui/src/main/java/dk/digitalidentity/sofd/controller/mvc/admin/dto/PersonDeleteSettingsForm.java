package dk.digitalidentity.sofd.controller.mvc.admin.dto;

import dk.digitalidentity.sofd.service.model.PersonDeletePeriod;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonDeleteSettingsForm {
	private PersonDeletePeriod personDeletePeriod;
}