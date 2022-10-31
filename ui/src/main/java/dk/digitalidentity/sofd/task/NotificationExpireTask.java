package dk.digitalidentity.sofd.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.NotificationService;
import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class NotificationExpireTask {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private NotificationService notificationService;

	// TODO: add a bit of fuzz here
	@Scheduled(cron = "0 30 5 * * ?")
	public void expireNotifications() {
		if (!configuration.getScheduled().isEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}

		notificationService.expire();
	}
}
