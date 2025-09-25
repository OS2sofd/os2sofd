package dk.digitalidentity.sofd.controller.mvc.admin;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import dk.digitalidentity.sofd.dao.model.OrgUnitType;
import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.service.OrgUnitService;

@RequireAdminAccess
@Controller
public class OrgUnitTypeController {

	@Autowired
	private OrgUnitService orgUnitService;

	@GetMapping("/ui/orgunittypes")
	public String listFunctionTypes(Model model) {
		List<OrgUnitType> orgUnitTypes = orgUnitService.getTypes().stream()
				.sorted(Comparator.comparingLong(OrgUnitType::getId))
				.collect(Collectors.toList());

		model.addAttribute("orgUnitTypes", orgUnitTypes);

		return "admin/orgUnitTypes/list";
	}
}
