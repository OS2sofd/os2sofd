package dk.digitalidentity.sofd.controller.mvc;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.service.InstitutionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@RequireReadAccess
@Controller
public class StudentController {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private InstitutionService institutionService;

	@GetMapping("/ui/student")
	public String list(Model model) {
		if (!configuration.getModules().getStudents().isEnabled()) {
			return "error";
		}

		model.addAttribute("institutions", institutionService.getAll());

		return "student/list";
	}
}
