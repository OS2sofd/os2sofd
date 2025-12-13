package dk.digitalidentity.sofd.controller.mvc;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import dk.digitalidentity.sofd.controller.mvc.dto.OrgUnitFutureChangeDTO;
import dk.digitalidentity.sofd.security.RequireLosAdminAccess;
import dk.digitalidentity.sofd.service.OrgUnitFutureChangesService;

@RequireLosAdminAccess
@Controller
public class FutureChangesController {

	@Autowired
	private OrgUnitFutureChangesService orgUnitfutureChangesService;

	@GetMapping("/ui/changes/orgunit")
	public String list(Model model) throws Exception {
		List<OrgUnitFutureChangeDTO> changesDTO = orgUnitfutureChangesService.getAllNotApplied().stream().map(c -> orgUnitfutureChangesService.convertToDTO(c)).toList();
		model.addAttribute("changes", changesDTO);
		return "orgunit/changes/list";
	}
}
