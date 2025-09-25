package dk.digitalidentity.sofd.controller.mvc;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import dk.digitalidentity.sofd.controller.mvc.dto.OrgUnitFutureChangeDTO;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.enums.OrgUnitAttribute;
import dk.digitalidentity.sofd.security.RequireLosAdminAccess;
import dk.digitalidentity.sofd.service.OrgUnitFutureChangesService;
import dk.digitalidentity.sofd.service.PersonService;

@RequireLosAdminAccess
@Controller
public class FutureChangesController {

	@Autowired
	private OrgUnitFutureChangesService orgUnitfutureChangesService;
	@Autowired
	private PersonService personService;

	@GetMapping("/ui/changes/orgunit")
	public String list(Model model) throws Exception {
		// convert to dto
		List<OrgUnitFutureChangeDTO> changesDTO = orgUnitfutureChangesService.getAllNotApplied().stream().map(OrgUnitFutureChangeDTO::new).toList();
		
		// Replace manager uuid with manager name for UI
		changesDTO.stream().filter(c -> c.getAttributeField() == OrgUnitAttribute.MANAGER).forEach(change -> {
			Person futureManager = personService.getByUuid(change.getAttributeValue());
			if (futureManager != null) {
				change.setAttributeValue(PersonService.getName(futureManager));
			}
		});

		model.addAttribute("changes", changesDTO);

		return "orgunit/changes/list";
	}
}
