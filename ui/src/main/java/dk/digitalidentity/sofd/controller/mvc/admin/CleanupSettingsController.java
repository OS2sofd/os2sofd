package dk.digitalidentity.sofd.controller.mvc.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import dk.digitalidentity.sofd.controller.mvc.admin.dto.PersonDeleteSettingsForm;
import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.service.SettingService;

@RequireAdminAccess
@Controller
public class CleanupSettingsController {

	@Autowired
	private SettingService settingService;

	@GetMapping(value = "/ui/admin/cleanup/persondeleteperiod")
	public String userDeletePeriod(Model model) {
		PersonDeleteSettingsForm personDeleteSettingsForm = new PersonDeleteSettingsForm();

		personDeleteSettingsForm.setPersonDeletePeriod(settingService.getPersonDeletePeriod());

		model.addAttribute("personDeleteSettingsForm", personDeleteSettingsForm);

		return "admin/cleanup/persondeleteperiod";
	}

	@PostMapping(value = "/ui/admin/cleanup/persondeleteperiod")
	public String updateUserDeleteSettings(Model model, @ModelAttribute("personDeleteSettingsForm") PersonDeleteSettingsForm personDeleteSettingsForm, RedirectAttributes redirectAttributes) {
		settingService.setPersonDeletePeriod(personDeleteSettingsForm.getPersonDeletePeriod());

		redirectAttributes.addFlashAttribute("saved", true);

		return "redirect:/ui/admin/cleanup/persondeleteperiod";
	}
}
