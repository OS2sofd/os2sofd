package dk.digitalidentity.sofd.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.OpusService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
public class OpusTask {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private OpusService opusService;

	// run once every 2 minutes
	@Scheduled(cron = "0 0/2 * * * ?")
	@Transactional(rollbackFor = Exception.class)
	public void handleOrders() {
		if (configuration.getScheduled().isEnabled() && configuration.getModules().getAccountCreation().getOpusHandler().isEnabled()) {
			log.debug("handleOrders");
			
			opusService.handleOrders();
		}
	}
	
	// run every midday (11-12)
	@Scheduled(cron = "${cron.opus.email:0 #{new java.util.Random().nextInt(60)} 11 * * ?}")
	public void updateEmails() {
		if (configuration.getScheduled().isEnabled() && configuration.getModules().getAccountCreation().getOpusHandler().isUpdateEmailWithoutIdM()) {
			log.info("Running OPUS bulk email update task");
			opusService.bulkUpdateEmail();
		}
	}
}
