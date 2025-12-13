package dk.digitalidentity.sofd.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.PersonService;
import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class ChosenNameCleanupTask {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private PersonService personService;

	// run once per day
	@Scheduled(fixedDelay = 24 * 60 * 60 * 1000)
	@Transactional
	public void cleanupChosenNames() {
		if (!configuration.getScheduled().isEnabled() || !configuration.getModules().getPerson().isResetChosenNameWhenInactive()) {
			return;
		}

		personService.removeChosenNameOnInactivePersons();
	}
}
