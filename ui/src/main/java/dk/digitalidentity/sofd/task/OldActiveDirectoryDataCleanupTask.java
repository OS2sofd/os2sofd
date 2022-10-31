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
public class OldActiveDirectoryDataCleanupTask {

	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired
	private PersonService personService;

	// TODO: move into NightBatchTask
	@Scheduled(cron = "0 22 2 * * ?")
	public void processChanges() {
		if (!configuration.getScheduled().isEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}

		personService.cleanupOldActiveDirectoryData();
	}
}
