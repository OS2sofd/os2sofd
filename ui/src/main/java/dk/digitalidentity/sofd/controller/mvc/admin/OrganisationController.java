package dk.digitalidentity.sofd.controller.mvc.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.service.OrganisationService;

@RequireAdminAccess
@Controller
public class OrganisationController {

    @Autowired
    private OrganisationService organisationService;

    @GetMapping("/ui/organisation")
    public String listOrganisations(Model model) {
        model.addAttribute("organisations", organisationService.getAll());

        return "organisation/list";
    }
}
