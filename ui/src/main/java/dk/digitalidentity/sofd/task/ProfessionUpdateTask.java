package dk.digitalidentity.sofd.task;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.ProfessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@EnableScheduling
public class ProfessionUpdateTask {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private ProfessionService professionService;

	// run every hour
	@Scheduled(fixedDelay = 60 * 60 * 1000, initialDelay = 5 * 1000)
	public void processProfessions() {
		if (!configuration.getScheduled().isEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}

		professionService.updateAllAffiliations();
	}
}
