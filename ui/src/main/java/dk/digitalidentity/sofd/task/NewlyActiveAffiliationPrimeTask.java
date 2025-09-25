package dk.digitalidentity.sofd.task;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.AffiliationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@EnableScheduling
@Component
@Slf4j
public class NewlyActiveAffiliationPrimeTask {

	@Autowired
	private AffiliationService affiliationService;

	@Autowired
	private SofdConfiguration configuration;

	@Scheduled(cron = "0 #{new java.util.Random().nextInt(55)} 3 * * ?")
	public void processOrgUnitChanges() {
		if (!configuration.getScheduled().isEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}

		SecurityUtil.fakeLoginSession();
		affiliationService.setNewlyActiveAffiliationsPrime();
	}
}
