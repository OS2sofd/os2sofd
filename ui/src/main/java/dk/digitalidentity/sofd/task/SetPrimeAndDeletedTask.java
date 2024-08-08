package dk.digitalidentity.sofd.task;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.PrimeService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class SetPrimeAndDeletedTask {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private PersonService personService;

	@Autowired
	private PrimeService primeService;
	
	@Autowired
	private SupportedUserTypeService userTypeService;

	@Scheduled(cron = "${cron.primeAndDeleted:0 0 6 * * ?}")
	@Transactional(rollbackFor = Exception.class)
	public void processChanges() {
		if (!configuration.getScheduled().isEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}
		log.info("Executing primeAndDeleted task");

		SecurityUtil.fakeLoginSession();

		List<Person> persons = personService.getAll();
		List<String> allUserTypes = userTypeService.getAllUserTypes();
		
		int counter = 0;
		for (Person person : persons) {
			counter ++;
			if( counter % 100 == 0) {
				log.debug("Handling person " + counter + " of " + persons.size());
			}
			boolean changes = primeService.setPrimeAffilation(person);
			
			if (person.getUsers() != null) {
				for (String userType : allUserTypes) {
					List<User> users = PersonService.getUsers(person).stream().filter(u -> u.getUserType().equals(userType)).collect(Collectors.toList());
					
					if (users != null && users.size() > 0) {
						changes = changes || primeService.setPrimeUser(users);
					}
				}
			}
			
			// if the person does not have any users, and does not have any prime affiliations (i.e. no active affiliations), then flip the delete flag
			boolean shouldBeDeleted = (person.getUsers().size() == 0 && !person.getAffiliations().stream().anyMatch(a -> a.isPrime()));
			if (shouldBeDeleted && !person.isDeleted()) {
				person.setDeleted(true);
				changes = true;
			}
			else if (!shouldBeDeleted && person.isDeleted()) {
				person.setDeleted(false);
				changes = true;
			}
			
			if (changes) {
				personService.save(person);
			}
		}
		log.info("Finished executing primeAndDeleted task");
	}
}