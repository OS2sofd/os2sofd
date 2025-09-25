package dk.digitalidentity.sofd.controller.rest;

import dk.digitalidentity.sofd.controller.mvc.dto.SupportedUserTypeDTO;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SupportedUserType;
import dk.digitalidentity.sofd.dao.model.enums.UsernameInfixType;
import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import dk.digitalidentity.sofd.service.UsernameGeneratorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RequireAdminAccess
@RestController
@Slf4j
public class UserTypeRestController {

	@Autowired
	private SupportedUserTypeService supportedUserTypeService;
    @Autowired
    private PersonService personService;
	@Autowired
	private UsernameGeneratorService usernameGeneratorService;

	@PostMapping("/rest/usertype/edit")
	@ResponseBody
	public ResponseEntity<String> editCategory(@RequestBody SupportedUserTypeDTO supportedUserTypeDTO) {
		// TODO: perhaps send this information back to the UI
		if (!valid(supportedUserTypeDTO)) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		
		SupportedUserType userType = new SupportedUserType();
		if (supportedUserTypeDTO.getId() > 0) {
			userType = supportedUserTypeService.findById(supportedUserTypeDTO.getId());

			if (userType == null) {
				log.warn("Requested SupportedUserType with ID:" + supportedUserTypeDTO.getId() + " not found.");
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
		}
		
		if (supportedUserTypeDTO.getDependsOn() > 0) {
			SupportedUserType otherUserType = supportedUserTypeService.findById(supportedUserTypeDTO.getDependsOn());
			if (otherUserType != null) {
				// TODO: should really do this recursively to avoid issues with circular dependencies
				if (otherUserType.getDependsOn() == null || otherUserType.getDependsOn().getId() != userType.getId()) {
					userType.setDependsOn(otherUserType);
				}
			}
		}
		else {
			userType.setDependsOn(null);
		}

		userType.setName(supportedUserTypeDTO.getName());
		userType.setCanOrder(supportedUserTypeDTO.isCanOrder());
		userType.setDaysToDeactivate(supportedUserTypeDTO.getDaysToDeactivate());
		userType.setDaysToDelete(supportedUserTypeDTO.getDaysToDelete());
		userType.setDaysBeforeToCreate(supportedUserTypeDTO.getDaysBeforeToCreate());
		userType.setMinutesDelay(supportedUserTypeDTO.getDependsOnDelay());
		userType.setCreateEnabled(supportedUserTypeDTO.isCreateEnabled());
		userType.setDeactivateEnabled(supportedUserTypeDTO.isDeactivateEnabled());
		userType.setDeleteEnabled(supportedUserTypeDTO.isDeleteEnabled());

		// log an error, so we get an alarm, and can deal with this change pro-actively
		if (!Objects.equals(userType.isSingleUserMode(), supportedUserTypeDTO.isSingleUserMode())) {
			log.error("Changed singleUserMode on: " + userType.getKey());
			userType.setSingleUserMode(supportedUserTypeDTO.isSingleUserMode());
		}
		
		// key can only be set when creating
		if (supportedUserTypeDTO.getId() == 0) {
			userType.setKey(supportedUserTypeDTO.getKey());
		}

		userType.setUsernameType(supportedUserTypeDTO.getUsernameType());
		userType.setUsernamePrefix(supportedUserTypeDTO.getPrefix());
		userType.setUsernamePrefixValue(supportedUserTypeDTO.getPrefixValue());
		userType.setUsernamePrefixExternalValue(supportedUserTypeDTO.getPrefixExternalValue());
		userType.setUsernameInfix(supportedUserTypeDTO.getInfix());
		userType.setUsernameInfixValue(supportedUserTypeDTO.getInfixValue());
		userType.setUsernameSuffix(supportedUserTypeDTO.getSuffix());
		userType.setUsernameSuffixValue(supportedUserTypeDTO.getSuffixValue());
		userType.setUsernameSuffixExternalValue(supportedUserTypeDTO.getSuffixExternalValue());
		userType.setUsernameTemplateString(supportedUserTypeDTO.getUsernameTemplateString());
		
		
		// if we set the username to match another usertype, we should depend on it, effectively overwriting any other choices
		if (userType.getUsernameInfix().equals(UsernameInfixType.SAME_AS_OTHER)) {
			SupportedUserType otherUserType = supportedUserTypeService.findById(Long.parseLong(userType.getUsernameInfixValue()));
			if (otherUserType != null) {
				// TODO: should really do this recursively to avoid issues with circular dependencies
				if (otherUserType.getDependsOn() == null || otherUserType.getDependsOn().getId() != userType.getId()) {
					userType.setDependsOn(otherUserType);
				}
			}
		}

		supportedUserTypeService.save(userType);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	private boolean valid(SupportedUserTypeDTO supportedUserTypeDTO) {
		if (supportedUserTypeDTO.getName() == null || supportedUserTypeDTO.getName().length() < 2) {
			log.warn("Requested SupportedUserType with name:" + supportedUserTypeDTO.getName() + " has invalid name");
			return false;
		}
		
		if (supportedUserTypeDTO.getDaysBeforeToCreate() < 0) {
			log.warn("Requested SupportedUserType with name:" + supportedUserTypeDTO.getName() + " has invalid daysBeforeToCreate");
			return false;
		}

		if (supportedUserTypeDTO.getDaysToDeactivate() < 0) {
			log.warn("Requested SupportedUserType with name:" + supportedUserTypeDTO.getName() + " has invalid daysToDeactivate");
			return false;
		}
		
		if (supportedUserTypeDTO.getDaysToDelete() < 0) {
			log.warn("Requested SupportedUserType with name:" + supportedUserTypeDTO.getName() + " has invalid daysToDelete");
			return false;
		}
		
		if (supportedUserTypeDTO.getDaysToDelete() > 0 && supportedUserTypeDTO.getDaysToDelete() < supportedUserTypeDTO.getDaysToDeactivate()) {
			log.warn("Requested SupportedUserType with name:" + supportedUserTypeDTO.getName() + " has invalid daysToDeactivate/daysToDelete combo");
			return false;
		}
		
		// key can only be set when creating
		if (supportedUserTypeDTO.getId() == 0) {
			String key = supportedUserTypeDTO.getKey();
			key = key.toUpperCase().replaceAll("[^A-Z_]", "");

			if (key.length() < 2) {
				log.warn("Requested SupportedUserType with Key: " + supportedUserTypeDTO.getKey() + " has invalid key");
				return false;
			}
			
			if (supportedUserTypeService.findByKey(key) != null) {
				log.warn("Requested SupportedUserType with Key: " + supportedUserTypeDTO.getKey() + " already exists");
				return false;
			}
		}

		return true;
	}

	@GetMapping("/rest/usertype/testTemplate")
	@ResponseBody
	public HttpEntity<String> testTemplate(@RequestParam String template) {
		var tempSupportedUserType = new SupportedUserType();
		tempSupportedUserType.setUsernameTemplateString(template);

		var testResult = "";
		var randomPeople = personService.getRandomPeople(10);
		for( var randomPerson : randomPeople ) {
			testResult += getTestResult(tempSupportedUserType, randomPerson);
		}

		return new ResponseEntity<>(testResult, HttpStatus.OK);
	}

	private String getTestResult(SupportedUserType supportedUserType, Person person) {
		if( person != null ) {
			var affiliation = person.getAffiliations().stream().filter(a -> a.isPrime()).findFirst().orElse(null);
			var userName = usernameGeneratorService.generateUsernameFromStringTemplate(supportedUserType, affiliation, person);
			return PersonService.getName(person) + ": " + userName + "<br />";
		}
		return "";
	}

}
