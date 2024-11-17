package dk.digitalidentity.sofd.controller.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.security.RequireDaoWriteAccess;
import dk.digitalidentity.sofd.service.AccountOrderService;
import dk.digitalidentity.sofd.service.PersonService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequireDaoWriteAccess
@RestController
public class ExemptIdmApi {

	@Autowired
	private AccountOrderService accountOrderService;

	@Autowired
	private PersonService personService;

	@GetMapping("/api/account/lock/{uuid}")
	public ResponseEntity<?> isAccountOrdersDisabled(@PathVariable("uuid") String uuid) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			log.warn("Could not find person object with uuid: " + uuid);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(person.isDisableAccountOrdersCreate(), HttpStatus.OK);
	}
	
	@GetMapping("/api/account/lock/enabled")
	public ResponseEntity<?> getPersonsWithLockEnabled() {
		List<String> persons = personService.getUuidsOfDisableAccountOrderPersons();

		return new ResponseEntity<>(persons, HttpStatus.OK);
	}

	@PostMapping("/api/account/lock/{uuid}/enable")
	public ResponseEntity<String> enableAccountOrders(@PathVariable("uuid") String uuid) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			log.warn("Could not find person object with uuid: " + uuid);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		person.setDisableAccountOrdersCreate(true);
		person.setDisableAccountOrdersDisable(true);
		person.setDisableAccountOrdersDelete(true);

		personService.save(person);
		accountOrderService.deletePendingCreateOrders(person);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/api/account/lock/{uuid}/disable")
	public ResponseEntity<String> disableAccountOrders(@PathVariable("uuid") String uuid) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			log.warn("Could not find person object with uuid: " + uuid);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		person.setDisableAccountOrdersCreate(false);
		person.setDisableAccountOrdersDisable(false);
		person.setDisableAccountOrdersDelete(false);

		personService.save(person);

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
