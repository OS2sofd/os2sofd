package dk.digitalidentity.sofd.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.AccountOrderApprovedService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
public class AccountOrderApprovedCleanupTask {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private AccountOrderApprovedService accountOrderApprovedService;

	// run once every sunday
	@Scheduled(cron = "0 #{new java.util.Random().nextInt(55)} 10 * * SUN")
	public void cleanup() {
		if (!configuration.getScheduled().isEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}
		
		log.info("Deleting old accountOrderApprovals");

		accountOrderApprovedService.deleteOlderThan13Months();
		
		log.info("Completed cleanup");
	}
}
