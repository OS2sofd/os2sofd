package dk.digitalidentity.sofd.task;

import java.time.LocalTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.EmailQueueService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
public class EmailQueueTask {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private EmailQueueService emailQueueService;

	// run once every 5 minutes
	@Scheduled(fixedDelay = 5 * 60 * 1000)
	public void processEmails() {
		if (!configuration.getScheduled().isEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}

		// don't run during the night - unless configured to
		if (!configuration.getScheduled().isMailDuringNightEnabled() && LocalTime.now().isAfter(LocalTime.of(22, 0)) || LocalTime.now().isBefore(LocalTime.of(5, 30))) {
			return;
		}

		emailQueueService.sendPending();
	}
}
