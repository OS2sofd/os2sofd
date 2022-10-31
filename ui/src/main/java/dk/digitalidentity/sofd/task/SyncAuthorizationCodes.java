package dk.digitalidentity.sofd.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.AuthorizationCodeService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
public class SyncAuthorizationCodes {
	
	@Autowired
	private AuthorizationCodeService authorizationCodeService;

	@Autowired
	private SofdConfiguration configuration;

	// run every Monday at 00:xx-04:xx
	@Scheduled(cron = "0 #{new java.util.Random().nextInt(55)} #{new java.util.Random().nextInt(4)} * * MON")
	public void execute() {
		if (!configuration.getScheduled().isEnabled()) {
			return;
		}
		
		log.info("Updating authorization code for all persons");
		authorizationCodeService.syncAll();
		log.info("Done updating authorization codes");
	}
}
