package dk.digitalidentity.sofd.controller.mvc.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.service.SubstituteContextService;

@RequireAdminAccess
@Controller
public class SubstituteContextController {

	@Autowired
	private SubstituteContextService substituteContextService;

	@GetMapping("/ui/substituteContext")
	public String listSubstituteContexts(Model model) {
		model.addAttribute("substituteContexts", substituteContextService.getAll());

		return "admin/substituteContext/list";
	}
}
