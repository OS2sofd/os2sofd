package dk.digitalidentity.sofd.controller.rest;

import java.util.Objects;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.controller.mvc.dto.ProfileDTO;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.security.RequireProfileAccess;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.PersonService;

@RequireProfileAccess
@RestController
public class ProfileRestController {
	
	@Autowired
	private PersonService personService;

	@PostMapping(path = {"/rest/profile/update"})
	public HttpEntity<?> updateProfile(@RequestBody @Valid ProfileDTO profileDTO, BindingResult bindingResult) throws Exception {
		if (bindingResult.hasErrors()) {
			return new ResponseEntity<>(bindingResult.getAllErrors(), HttpStatus.BAD_REQUEST);
		}
		
		Person person = personService.getLoggedInPerson();
		if (person == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		
		if (!Objects.equals(profileDTO.getChosenName(), person.getChosenName())) {
			person.setChosenName(profileDTO.getChosenName());

			Authentication authentication = SecurityUtil.getLoginSession();
			try {
				SecurityUtil.fakeLoginSession();
				
				person = personService.save(person);
			}
			finally {
				SecurityUtil.setLoginSession(authentication);
			}
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
