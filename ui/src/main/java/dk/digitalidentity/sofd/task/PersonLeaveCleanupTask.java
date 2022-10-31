package dk.digitalidentity.sofd.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.PersonService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
public class PersonLeaveCleanupTask {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private PersonService personService;

	// run at 01:30, to ensure we cleanup those that expired the day before, but before
	// the start at work
	@Scheduled(cron = "0 30 1 * * ?")
	public void processEmails() {
		if (!configuration.getScheduled().isEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}

		personService.handlePersonsOnLeave();
	}
}
