package dk.digitalidentity.sofd.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.AffiliationService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
public class DeleteOldAffiliationsTask {
	
	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private AffiliationService affiliationService;
	
	@Scheduled(cron = "0 35 8 * * ?")
	public void deleteOldAffiliations() {
		if (!configuration.getScheduled().isEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}

		if (!configuration.getScheduled().getDeleteOldAffiliations().isEnabled()) {
			log.debug("DeleteOldAffiliationsTask is disabled.");
			return;
		}

		affiliationService.deleteOldAffiliations();
	}
}
