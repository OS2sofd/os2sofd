package dk.digitalidentity.sofd.controller.mvc.admin;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import dk.digitalidentity.sofd.dao.model.SupportedUserType;
import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;

@RequireAdminAccess
@Controller
public class UserTypesController {

	@Autowired
	private SupportedUserTypeService supportedUserTypeService;

	@GetMapping("/ui/usertype")
	public String listFunctionTypes(Model model) {
		List<SupportedUserType> userTypes = supportedUserTypeService.findAllBypassCache().stream()
				.sorted(Comparator.comparingLong(SupportedUserType::getId))
				.collect(Collectors.toList());

		model.addAttribute("userTypes", userTypes);

		return "admin/userTypes/list";
	}
}
