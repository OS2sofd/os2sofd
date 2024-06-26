package dk.digitalidentity.sofd.controller.api;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.mapping.PersonUserMapping;
import dk.digitalidentity.sofd.security.RequireApiWriteAccess;
import dk.digitalidentity.sofd.security.RequireDaoWriteAccess;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;

@RequireDaoWriteAccess
@RestController
public class MitIDErhvervApiController {

	@Autowired
	private PersonService personService;
	
	public record MitIDErhvervStatusEntry (String cpr, String samAccountName, String nemloginUserUuid, boolean active) { }
	public record MitIDErhvervStatus (String domain, List<MitIDErhvervStatusEntry> entries) {  }
	
	@RequireApiWriteAccess
	@PostMapping("/api/nemlogin")
	public ResponseEntity<String> postMitIDErhvervAccounts(@RequestBody MitIDErhvervStatus status) throws Exception {
		List<Person> persons = personService.getAll();

		Map<String, List<MitIDErhvervStatusEntry>> entriesByCpr = status.entries().stream().collect(Collectors.groupingBy(e -> e.cpr()));
		
		for (Person person : persons) {
			boolean changes = false;
			
			List<MitIDErhvervStatusEntry> entries = entriesByCpr.get(person.getCpr());
			if (entries == null) {
				entries = new ArrayList<>();
			}

			// existing users
			Set<User> mitIdErhvervUsers = PersonService.getUsers(person).stream()
					.filter(u -> SupportedUserTypeService.isMitIDErhverv(u.getUserType()))
					.collect(Collectors.toSet());

			// add / update
			for (MitIDErhvervStatusEntry entry : entries) {
				boolean exists = false;
				
				for (User user : mitIdErhvervUsers) {					
					if (Objects.equals(user.getUserId(), entry.nemloginUserUuid())) {

						// exists! modify state if needed
						if (user.isDisabled() != !entry.active()) {
							user.setDisabled(!entry.active());

							changes = true;
						}
						
						exists = true;
						break;
					}
				}
				
				if (!exists) {
					User user = new User();
					user.setDisabled(!entry.active());
					user.setMaster("MitIDErhverv");
					user.setMasterId(entry.samAccountName());
					user.setUserId(entry.nemloginUserUuid());
					user.setUserType(SupportedUserTypeService.getMitIDErhvervUserType());
					user.setUuid(UUID.randomUUID().toString());
					
					PersonUserMapping mapping = new PersonUserMapping();
					mapping.setPerson(person);
					mapping.setUser(user);
					
					person.getUsers().add(mapping);

					changes = true;
				}
			}

			for (Iterator<PersonUserMapping> iterator = person.getUsers().iterator(); iterator.hasNext();) {
				PersonUserMapping personUserMapping = iterator.next();
				
				if (!SupportedUserTypeService.isMitIDErhverv(personUserMapping.getUser().getUserType())) {
					continue;
				}
				
				User user = personUserMapping.getUser();
				if (!entries.stream().anyMatch(e -> Objects.equals(e.nemloginUserUuid(), user.getUserId()))) {
					iterator.remove();

					changes = true;
				}
			}
			
			if (changes) {
				personService.save(person);
			}
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
