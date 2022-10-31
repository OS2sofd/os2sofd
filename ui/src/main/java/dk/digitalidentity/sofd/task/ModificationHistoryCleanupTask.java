package dk.digitalidentity.sofd.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.ModificationHistoryService;
import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class ModificationHistoryCleanupTask {

	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired
	private ModificationHistoryService modificationHistoryService;

	@Scheduled(cron = "0 0 1 * * ?")
	@Transactional(rollbackFor = Exception.class)
	public void processChanges() {
		if (!configuration.getScheduled().isEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}

		modificationHistoryService.removeModificationHistoryOlderThan(configuration.getScheduled().getModificationHistoryCleanup().getDays());
	}
}
