package dk.digitalidentity.sofd.controller.mvc.admin;

import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import dk.digitalidentity.sofd.controller.mvc.admin.dto.SOFDAccountDTO;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SOFDAccount;
import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SOFDAccountService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequireAdminAccess
@Controller
public class SOFDAccountController {

	@Autowired
	private PersonService personService;

	@Autowired
	private SOFDAccountService sofdAccountService;

	@GetMapping("/ui/sofdaccounts")
	public String sofdAccounts(Model model) {
		model.addAttribute("accounts", sofdAccountService.findAll()
				.stream()
				.map(a -> new SOFDAccountDTO(a))
				.collect(Collectors.toList()));

		return "admin/sofdaccounts/list";
	}
	
	@GetMapping("/ui/sofdaccounts/new")
	public String newSofdAccount(Model model) {
		model.addAttribute("account", new SOFDAccountDTO());

		return "admin/sofdaccounts/new";
	}
	
	@PostMapping("/ui/sofdaccounts/new")
	public String createSofdAccount(Model model, @Valid @ModelAttribute("account") SOFDAccountDTO account, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			model.addAttribute(bindingResult.getAllErrors());
			model.addAttribute("account", account);

			return "admin/sofdaccounts/new";
		}
		
		Person person = personService.getByUuid(account.getPersonUUID());
		if (person == null) {
			bindingResult.addError(new FieldError("account", "personUUID", "Personen findes ikke!"));
			model.addAttribute(bindingResult.getAllErrors());
			model.addAttribute("account", account);

			return "admin/sofdaccounts/new";
		}
		
		if (!isUniqueUserId(account.getUserId())) {
			bindingResult.addError(new FieldError("account", "userId", "Bruger-id'et er allerede i brug"));
			model.addAttribute(bindingResult.getAllErrors());
			model.addAttribute("account", account);

			return "admin/sofdaccounts/new";
		}
		
		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		
		SOFDAccount sofdAccount = new SOFDAccount();
		sofdAccount.setPerson(person);
		sofdAccount.setUserId(account.getUserId());
		sofdAccount.setPassword(passwordEncoder.encode(account.getPassword()));
		sofdAccountService.save(sofdAccount);

		return "redirect:/ui/sofdaccounts";
	}
	
	@GetMapping("/ui/sofdaccounts/edit/{id}")
	public String editSofdAccount(Model model, @PathVariable("id") long id) {
		SOFDAccount sofdAccount = sofdAccountService.findById(id);
		if (sofdAccount == null) {
			log.warn("Could not find SOFD Account with ID " + id);
			return "redirect:/ui/sofdaccounts";
		}

		model.addAttribute("account", new SOFDAccountDTO(sofdAccount));

		return "admin/sofdaccounts/edit";
	}
	
	@PostMapping("/ui/sofdaccounts/edit/{id}")
	public String updateSofdAccount(Model model, @PathVariable("id") long id, @ModelAttribute("account") SOFDAccountDTO account, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			model.addAttribute(bindingResult.getAllErrors());
			model.addAttribute("account", account);

			return "admin/sofdaccounts/edit";
		}
		
		SOFDAccount sofdAccount = sofdAccountService.findById(id);
		if (sofdAccount == null) {
			log.warn("Could not find SOFD Account with ID " + id);
			return "redirect:/ui/sofdaccounts";
		}

		if (account.getPassword() == null || account.getPassword().length() < 8 || account.getPassword().length() > 36) {
			bindingResult.addError(new FieldError("account", "password", "Der skal vælges et kodeord på mindst 8 tegn (og max 36 tegn)"));
			model.addAttribute(bindingResult.getAllErrors());
			model.addAttribute("account", account);

			return "admin/sofdaccounts/edit";
		}

		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

		sofdAccount.setPassword(passwordEncoder.encode(account.getPassword()));
		sofdAccountService.save(sofdAccount);

		return "redirect:/ui/sofdaccounts";
	}
	
	private boolean isUniqueUserId(String userId) {
		return sofdAccountService.findByUserId(userId) == null;
	}
}
