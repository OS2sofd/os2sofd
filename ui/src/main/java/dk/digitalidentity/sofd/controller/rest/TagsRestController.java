package dk.digitalidentity.sofd.controller.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.dao.model.AccountOrder;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.security.RequireUserEditOrManager;
import dk.digitalidentity.sofd.service.AccountOrderService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.TagsService;

@RequireUserEditOrManager
@RestController
public class TagsRestController {

	@Autowired
	private PersonService personService;

	@Autowired
	private TagsService tagsService;

	@Autowired
	private AccountOrderService accountOrderService;
	
	@GetMapping("/rest/tags/{tagValue}/valueForPerson/{personUuid}")
	@ResponseBody
	public HttpEntity<String> getValueForPerson(@PathVariable("tagValue") String tagValue, @PathVariable("personUuid") String personUuid) {
		Person person = personService.getByUuid(personUuid);
		if (person == null) {
			return new ResponseEntity<>("No person with uuid " + personUuid, HttpStatus.NOT_FOUND);
		}
		
		String customValue = tagsService.getTagValueForPersonsPrimaryAffiliation(person, tagValue);

		return new ResponseEntity<>(customValue, HttpStatus.OK);
	}

	// used by custom approval forms to check for a tag (with or without a custom value) on the orgunit
	// of the affiliation that triggered the account order
	@GetMapping("/rest/tags/{tagValue}/existsForOrder/{orderId}")
	@ResponseBody
	public HttpEntity<Boolean> existsForOrder(@PathVariable("tagValue") String tagValue, @PathVariable("orderId") long orderId) {
		AccountOrder order = accountOrderService.findById(orderId);
		if (order == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		Person person = personService.getByUuid(order.getPersonUuid());

		return new ResponseEntity<>(tagsService.hasTagOnAccountOrderOrgUnit(order, person, tagValue), HttpStatus.OK);
	}
}
