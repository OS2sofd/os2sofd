package dk.digitalidentity.sofd.controller.mvc.admin;

import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.service.OrganisationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@RequireAdminAccess
@Controller
public class ProfessionController {

    @Autowired
    private OrganisationService organisationService;

    @GetMapping("/ui/admin/professions")
    public String listFunctionTypes(Model model) {
        model.addAttribute("organisations", organisationService.getAll());
        return "admin/professions/list";
    }
}
