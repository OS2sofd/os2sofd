package dk.digitalidentity.sofd.controller.mvc;

import java.util.List;

import dk.digitalidentity.sofd.dao.model.enums.OrgUnitChangeType;
import dk.digitalidentity.sofd.service.TagsService;
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

	@Autowired
	private TagsService tagsService;

	@GetMapping("/ui/changes/orgunit")
	public String list(Model model) throws Exception {
		// convert to dto
		List<OrgUnitFutureChangeDTO> changesDTO = orgUnitfutureChangesService.getAllNotApplied().stream().map(OrgUnitFutureChangeDTO::new).toList();


		for (var change : changesDTO) {
			if (change.getChangeType() == OrgUnitChangeType.UPDATE_ATTRIBUTE) {
				if (change.getAttributeField() == OrgUnitAttribute.MANAGER) {
					// replace manager uuid with manager name for UI
					Person futureManager = personService.getByUuid(change.getAttributeValue());
					if (futureManager != null) {
						change.setAttributeValue(PersonService.getName(futureManager));
					}
				}
			}
			if (change.getChangeType() == OrgUnitChangeType.ADD_TAG || change.getChangeType() == OrgUnitChangeType.REMOVE_TAG) {
				// add tag details
				var tag = tagsService.findById(change.getTagId());
				if (tag != null) {
					var operation = change.getChangeType() == OrgUnitChangeType.ADD_TAG ? "tilføjes" : "fjernes";
					var details = "Tag '" + tag.getValue() + "' " + operation;
					if (change.getTagValue() != null) {
						details += " med værdien '" + change.getTagValue() + "'";
					}
					change.setDetails(details);
				}
			}
		}

		model.addAttribute("changes", changesDTO);
		return "orgunit/changes/list";
	}
}
