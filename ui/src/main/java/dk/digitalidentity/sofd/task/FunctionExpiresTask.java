package dk.digitalidentity.sofd.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.FunctionAssignmentService;

@EnableScheduling
@Component
public class FunctionExpiresTask {
	
	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired 
	private FunctionAssignmentService functionAssignmentService;

	// every day 7.15 am
	@Scheduled(cron = "0 15 7 * * ?")
	public void run() {
		if (!configuration.getScheduled().isEnabled()) {
			return;
		}
		
		if (configuration.getModules().getFunctionHierarchy().isEnabled()) {
			functionAssignmentService.generateFunctionAssignmentExpiringNotifications();
		}
	}
}
