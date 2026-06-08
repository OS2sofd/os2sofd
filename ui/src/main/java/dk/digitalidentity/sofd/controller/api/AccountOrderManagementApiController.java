package dk.digitalidentity.sofd.controller.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.dao.model.AccountOrder;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SupportedUserType;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderType;
import dk.digitalidentity.sofd.security.RequireApiWriteAccess;
import dk.digitalidentity.sofd.service.AccountOrderService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequireApiWriteAccess
public class AccountOrderManagementApiController {

	@Autowired
	private AccountOrderService accountOrderService;

	@Autowired
	private PersonService personService;

	@Autowired
	private SupportedUserTypeService supportedUserTypeService;

	@PostMapping("/api/accountordermanagement/deactivate")
	public ResponseEntity<?> deactivate(@RequestParam String userType, @RequestParam String userId, @RequestParam String personUuid) {

		Person person = personService.getByUuid(personUuid);
		if (person == null) {
			return new ResponseEntity<>("Person with uuid " + personUuid + " not found.", HttpStatus.NOT_FOUND);
		}
		
		SupportedUserType supportedUserType = supportedUserTypeService.findByKey(userType);
		if (supportedUserType == null) {
			log.warn("UserType does not exist: " + userType);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		User user = person.getUsers().stream().map(u -> u.getUser()).filter(u -> u.getUserId().equals(userId) && u.getUserType().equals(userType)).findAny().orElse(null);
		if (user == null) {
			return new ResponseEntity<>("Person does not have user with userId " + userId + " and userType " + userType, HttpStatus.NOT_FOUND);
		}
		
		if (user.isDisabled()) {
			return new ResponseEntity<>("User is already deactivated", HttpStatus.BAD_REQUEST);
		}
		
		List<AccountOrder> accountOrders = accountOrderService.findPendingDeactivation(person);
		long matchOrders = accountOrders.stream().filter(a -> a.getRequestedUserId().equals(userId) && a.getUserType().equals(userType)).count();
		if (matchOrders > 0) {
			return new ResponseEntity<>("A deactivate account order on this user already exists", HttpStatus.BAD_REQUEST);
		}
		
		AccountOrder order = accountOrderService.deactivateOrDeleteAccountOrder(AccountOrderType.DEACTIVATE, person, user.getEmployeeId(), userType, userId);
		
		accountOrderService.save(order);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
