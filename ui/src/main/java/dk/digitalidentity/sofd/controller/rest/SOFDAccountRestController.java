package dk.digitalidentity.sofd.controller.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.controller.mvc.admin.dto.SOFDAccountDTO;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SOFDAccount;
import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SOFDAccountService;
import dk.digitalidentity.sofd.telephony.controller.rest.dto.AutoCompleteResult;
import dk.digitalidentity.sofd.telephony.controller.rest.dto.ValueData;
import lombok.extern.slf4j.Slf4j;

@RequireAdminAccess
@RestController
@Slf4j
public class SOFDAccountRestController {

	@Autowired
	private PersonService personService;

	@Autowired
	private SOFDAccountService sofdAccountService;

	@GetMapping(value = "/rest/sofdAccount/search/person")
	@ResponseBody
	public ResponseEntity<?> searchPerson(@RequestParam("query") String term) {
		List<Person> persons = personService.findTop10ByName(term);

		List<ValueData> suggestions = new ArrayList<>();
		for (Person person : persons) {
			StringBuilder builder = new StringBuilder();
			builder.append(PersonService.getName(person));
			builder.append(" - ");
			builder.append(PersonService.maskCpr(person.getCpr()));

			Optional<Affiliation> primeAffiliation = person.getAffiliations().stream().filter(a -> a.isPrime()).findFirst();
			if (primeAffiliation.isPresent()) {
				builder.append(" (" + primeAffiliation.get().getCalculatedOrgUnit().getName() + ")");
			}

			ValueData vd = new ValueData();
			vd.setValue(builder.toString());
			vd.setData(person.getUuid());

			suggestions.add(vd);
		}

		AutoCompleteResult result = new AutoCompleteResult();
		result.setSuggestions(suggestions);

		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@PostMapping("/rest/sofdAccount/delete")
	@ResponseBody
	public ResponseEntity<String> deleteSOFDAccount(@RequestBody SOFDAccountDTO sofdAccountDTO) {
		SOFDAccount existingAccount = sofdAccountService.findById(sofdAccountDTO.getId());
		if (existingAccount == null) {
			log.warn("Account with id=" + sofdAccountDTO.getId() + " doesn't exist");

			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		sofdAccountService.delete(existingAccount);

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
