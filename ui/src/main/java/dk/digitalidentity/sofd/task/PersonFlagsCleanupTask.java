package dk.digitalidentity.sofd.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.PersonService;
import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class PersonFlagsCleanupTask {

	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired
	private PersonService personService;

	// TODO: add some random fuzz here
	@Scheduled(cron = "${cron.flagcleanup.task:0 30 10 * * ?}")
	public void cleanup() {
		if (!configuration.getScheduled().isEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}

		if(!configuration.getScheduled().getFlagCleanup().isEnabled()) {
			log.debug("Flag cleanup is not enabled");
			return;
		}

		personService.cleanupDeletedFlag();
	}
}
