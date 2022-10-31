package dk.digitalidentity.sofd.task;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.OrgUnitFutureChangesService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@EnableScheduling
@Slf4j
public class ApplyFutureChangesTask {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private OrgUnitFutureChangesService orgUnitFutureChangesService;

	@Scheduled(cron = "0 0 3 * * *")
	@Transactional(rollbackFor = Exception.class)
	public void saveFutureChanges() {
		if (!configuration.getScheduled().isEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}

		if (!configuration.getModules().getLos().isEnabled() || !configuration.getModules().getLos().isFutureOrgsEnabled()) {
			return;
		}

		orgUnitFutureChangesService.mergeFutureChanges();
	}
}
