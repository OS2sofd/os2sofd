package dk.digitalidentity.sofd.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.listener.EntityListenerService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@EnableScheduling
@Component
public class EntityListenerTask {

	@Autowired
	private EntityListenerService entityListenerService;
	
	@Autowired
	private SofdConfiguration configuration;
	
	// run every 2 minutes
	@Scheduled(fixedDelay = 2 * 60 * 1000)
	public void run() {
		if (!configuration.getScheduled().isEnabled()) {
			return;
		}
		
		log.debug("run");

		entityListenerService.emit();
	}
}
