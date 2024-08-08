package dk.digitalidentity.sofd.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.WorkplaceService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
public class WorkplaceModificationHistoryJobTask {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private WorkplaceService workplaceService;

	// run every night
	@Scheduled(cron = "0 #{new java.util.Random().nextInt(55)} 1 * * ?")
	public void processWorkplaceModifications() {
		if (!configuration.getModules().getAffiliationWorkplaces().isEnabled()) {
			return;
		}

		if (!configuration.getScheduled().isEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}

		workplaceService.findWorkplaceModifications();
	}
}
