package dk.digitalidentity.sofd.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.SettingService;
import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class ScheduledTaskCheckerTask {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private SettingService settingService;

    @Scheduled(cron = "0 0 3 ? * *")
    public void flagScheduledTasksRunning() {
    	
    	// normal check - only runs on SOFD #1
		if (!configuration.getScheduled().isEnabled()) {
			return;
		}

		settingService.setScheduledTasksRunning();
    }
    
    @Scheduled(cron = "0 0 4 ? * *")
    public void checkScheduledTasksRunning() {
    	
    	// reverse check - only runs on SOFD #2
		if (configuration.getScheduled().isEnabled()) {
			return;
		}

		if (!settingService.isScheduledTasksRunning()) {
			log.error("Scheduled tasks are not running on SOFD #1");
		}
		
		settingService.clearScheduledTasksRunning();
    }
}
