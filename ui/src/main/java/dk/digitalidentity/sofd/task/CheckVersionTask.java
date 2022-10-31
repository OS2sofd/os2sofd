package dk.digitalidentity.sofd.task;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.AppManagerService;
import dk.digitalidentity.sofd.service.model.ApplicationApiDTO;


@Component
@EnableScheduling
public class CheckVersionTask {

	@Autowired
	private AppManagerService appManagerService;

	@Autowired
	private SofdConfiguration configuration;

	// check for a new version once per hour
	@Scheduled(initialDelay = 1000, fixedDelay = 60 * 60 * 1000)
	public void checkVersion() {
		List<ApplicationApiDTO> applications = appManagerService.getApplications();
		if (applications == null) {
			return;
		}

		for (ApplicationApiDTO app : applications) {
			if (Objects.equals(app.getIdentifier(), "sofdcore")) {
				configuration.setLatestVersion(app.getNewestVersion());
			}
		}
	}
}