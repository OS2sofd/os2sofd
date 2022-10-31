package dk.digitalidentity.sofd.controller.mvc.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.service.ChartService;

@RequireAdminAccess
@Controller
public class ChartAdminController {

	@Autowired
	private ChartService chartService;

	@GetMapping("/ui/admin/chart/list")
	public String listCharts(Model model) {
		model.addAttribute("orgDiagrams", chartService.getAll());
		model.addAttribute("defaultStyle", chartService.getDefaultStyle());
		return "chart/list";
	}
}
