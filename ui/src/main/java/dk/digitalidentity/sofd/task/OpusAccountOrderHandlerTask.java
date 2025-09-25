package dk.digitalidentity.sofd.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.OpusService;

@Component
@EnableScheduling
public class OpusAccountOrderHandlerTask {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private OpusService opusService;

	// run once every 2 minutes
	@Scheduled(cron = "0 0/2 * * * ?")
	@Transactional(rollbackFor = Exception.class)
	public void processChanges() {
		if (configuration.getScheduled().isEnabled() && configuration.getModules().getAccountCreation().getOpusHandler().isEnabled()) {
			opusService.handleOrders();
		}
	}
}
