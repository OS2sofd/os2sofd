package dk.digitalidentity.sofd.controller.mvc.admin;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.service.InstitutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@RequireAdminAccess
@Controller
public class InstitutionController {

	@Autowired
	private SofdConfiguration config;

	@Autowired
	private InstitutionService institutionService;

	@GetMapping("/ui/institutions")
	public String listFunctions(Model model) {
		if (!config.getModules().getStudents().isEnabled()) {
			return "error";
		}

		model.addAttribute("institutions", institutionService.getAll());

		return "admin/institutions/list";
	}
}
