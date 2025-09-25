package dk.digitalidentity.sofd.task;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.service.AppManagerService;
import dk.digitalidentity.sofd.service.VersionService;
import dk.digitalidentity.sofd.service.model.ApplicationApiDTO;


@Component
@EnableScheduling
public class UpdateLatestVersionTask {

	@Autowired
	private AppManagerService appManagerService;

	@Autowired
	private VersionService versionService;

	// check for a new version once per hour
	@Scheduled(initialDelay = 1000, fixedDelay = 60 * 60 * 1000)
	public void updateLatestVersion() {
		List<ApplicationApiDTO> applications = appManagerService.getApplications();
		if (applications == null) {
			return;
		}

		for (ApplicationApiDTO app : applications) {
			if (Objects.equals(app.getIdentifier(), "sofdcore")) {
				versionService.setLatestVersion(app.getNewestVersion());
			}
		}
	}

	@Scheduled(cron = "0 0 6 ? * *")
	public void verifyAutoUpdateStatus() {
		versionService.verifyAutoUpdateStatus();
	}

}