package dk.digitalidentity.sofd.controller.mvc;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.admin.dto.LoginForm;
import dk.digitalidentity.sofd.dao.model.SOFDAccount;
import dk.digitalidentity.sofd.dao.model.enums.EventType;
import dk.digitalidentity.sofd.log.AuditLogger;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.SOFDAccountService;

@Controller
public class LocalLoginController {

	@Autowired
	private SOFDAccountService sofdAccountService;
	
	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired
	private AuditLogger auditLogger;

	@GetMapping("/login")
	public String loginGet(Model model) {
		if (!configuration.getModules().getLocalLogin().isEnabled()) {
			return "redirect:/";
		}

		model.addAttribute("loginForm", new LoginForm());

		return "login";
	}

	@PostMapping("/login")
	public String loginPost(Model model, @ModelAttribute("loginForm") LoginForm loginForm, final BindingResult bindingResult) {
		if (!configuration.getModules().getLocalLogin().isEnabled()) {
			return "redirect:/";
		}

		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

		SOFDAccount user = sofdAccountService.findByUserId(loginForm.getUsername());
		if (user != null) {
			if (passwordEncoder.matches(loginForm.getPassword(), user.getPassword())) {
				SecurityUtil.loginPerson(user.getPerson(), user.getUserId());

				auditLogger.log(user.getPerson(), EventType.LOGIN, null);
				return "redirect:/ui/sms/message";
			}
			else {
				auditLogger.log(user.getPerson(), EventType.FAILED_LOGIN, null);
			}
		}

		bindingResult.rejectValue("password", "mvc.errors.login.password.wrong");
		model.addAttribute("form", new LoginForm());

		return "login";
	}
}
