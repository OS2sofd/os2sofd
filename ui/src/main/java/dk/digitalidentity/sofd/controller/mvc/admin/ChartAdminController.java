package dk.digitalidentity.sofd.controller.mvc.admin;

import dk.digitalidentity.sofd.security.RequireAdminOrChartEditorAccess;
import dk.digitalidentity.sofd.service.OrganisationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import dk.digitalidentity.sofd.service.ChartService;

@RequireAdminOrChartEditorAccess
@Controller
public class ChartAdminController {

	@Autowired
	private ChartService chartService;
    @Autowired
    private OrganisationService organisationService;

	@GetMapping("/ui/admin/chart/list")
	public String listCharts(Model model) {
		model.addAttribute("orgDiagrams", chartService.getAll());
		model.addAttribute("defaultStyle", chartService.getDefaultStyle());
		model.addAttribute("organisations", organisationService.getAll());
		return "chart/list";
	}
}
