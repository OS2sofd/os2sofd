package dk.digitalidentity.sofd.controller.mvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.security.RequireProfileAccess;
import dk.digitalidentity.sofd.service.PersonService;

@RequireProfileAccess
@Controller(value = "MyProfileController")
public class ProfileController {
	
	@Autowired
	private PersonService personService;
	
	@GetMapping("/ui/profile")
	public String getProfile(Model model) {
		Person person = personService.getLoggedInPerson();
		if (person == null) {
			return "redirect:/";
		}
		model.addAttribute("person", person);

		return "profile/myProfile";
	}

	@GetMapping("/ui/profile/core/{type}")
	public String getCoreFragment(Model model, @PathVariable("type") String type) {
		Person person = personService.getLoggedInPerson();
		if (person == null) {
			return "redirect:/";
		}

		model.addAttribute("person", person);

		if (type.equals("edit")) {			
			return "profile/fragments/profile_core_edit :: profileCoreEdit";
		}

		return "profile/fragments/profile_core_view :: profileCoreView";
	}
}
