package dk.digitalidentity.sofd.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.FetchClientVersionsService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
public class FetchClientVersionsTask {
	
	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private FetchClientVersionsService service;
	
	// run every hour
	@Scheduled(fixedDelay = 60 * 60 * 1000)
	//@Scheduled(fixedDelay = 10 * 1000)
	public void fetchClientVersions() {
		if (!configuration.getScheduled().isEnabled()) {
			return;
		}

		service.executeTask();
	}
}
