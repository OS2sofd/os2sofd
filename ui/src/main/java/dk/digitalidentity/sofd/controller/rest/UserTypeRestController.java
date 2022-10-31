package dk.digitalidentity.sofd.controller.rest;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.controller.mvc.dto.SupportedUserTypeDTO;
import dk.digitalidentity.sofd.dao.model.SupportedUserType;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class UserTypeRestController {

	@Autowired
	private SupportedUserTypeService supportedUserTypeService;

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

		// log an error, so we get an alarm, and can deal with this change pro-actively
		if (!Objects.equals(userType.isSingleUserMode(), supportedUserTypeDTO.isSingleUserMode())) {
			log.error("Changed singleUserMode on: " + userType.getKey());
			userType.setSingleUserMode(supportedUserTypeDTO.isSingleUserMode());
		}
		
		// key can only be set when creating
		if (supportedUserTypeDTO.getId() == 0) {
			userType.setKey(supportedUserTypeDTO.getKey());
		}

		userType.setUsernamePrefix(supportedUserTypeDTO.getPrefix());
		userType.setUsernamePrefixValue(supportedUserTypeDTO.getPrefixValue());
		userType.setUsernameInfix(supportedUserTypeDTO.getInfix());
		userType.setUsernameInfixValue(supportedUserTypeDTO.getInfixValue());
		userType.setUsernameSuffix(supportedUserTypeDTO.getSuffix());
		userType.setUsernameSuffixValue(supportedUserTypeDTO.getSuffixValue());

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
}
