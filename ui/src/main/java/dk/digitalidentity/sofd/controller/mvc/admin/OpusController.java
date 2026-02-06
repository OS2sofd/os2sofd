package dk.digitalidentity.sofd.controller.mvc.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.security.RequireAdminAccess;

@RequireAdminAccess
@Controller
public class OpusController {

	@Autowired
	private SofdConfiguration configuration;

	@GetMapping(value = "/ui/admin/opusfilter")
	public String opusFilter(Model model) {
		model.addAttribute("losIds", configuration.getIntegrations().getOpus().getLosIds());
		model.addAttribute("positionIds", configuration.getIntegrations().getOpus().getPositionIds());
		model.addAttribute("positionNames", configuration.getIntegrations().getOpus().getPositionNames());
		model.addAttribute("invalidPositionNames", configuration.getIntegrations().getOpus().getInvalidPositionNames());
		model.addAttribute("orgUnitInfix", configuration.getIntegrations().getOpus().getOrgUnitInfix());

		return "admin/opus/filter";
	}
}
