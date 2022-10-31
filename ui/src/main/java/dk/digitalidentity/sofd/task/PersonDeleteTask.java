package dk.digitalidentity.sofd.task;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SettingService;
import dk.digitalidentity.sofd.service.model.PersonDeletePeriod;
import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class PersonDeleteTask {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private SettingService settingService;

	@Autowired
	private PersonService personService;

	@Scheduled(cron =  "0 0 15 * * SAT")
	public void processChanges() {
		if (!configuration.getScheduled().isEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}

		int months = 0;
		PersonDeletePeriod interval = settingService.getPersonDeletePeriod();
		switch (interval) {
			case MONTH_6:
				months = 6;
				break;
			case MONTH_12:
				months = 12;
				break;
			case MONTH_36:
				months = 36;
				break;
			case MONTH_60:
				months = 60;
				break;
			case NEVER:
				return;
			default:
				return;
		}

		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		cal.add(Calendar.MONTH, -1 * months);
		Date beforeDate = cal.getTime();

		List<Person> deletedPersons = personService.findAllDeleted();

		ArrayList<Person> toBeDeleted = new ArrayList<>();
		for (Person person : deletedPersons) {
			if (person.getLastChanged() == null || person.getLastChanged().before(beforeDate)) {
				log.info("Physical delete of " + PersonService.getName(person) + ", uuid=" + person.getUuid());

				toBeDeleted.add(person);
			}
		}
		
		personService.hardDeletePersons(toBeDeleted);
		personService.deletePersonsLogs(toBeDeleted);
	}
}
