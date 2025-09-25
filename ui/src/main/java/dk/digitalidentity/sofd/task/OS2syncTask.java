package dk.digitalidentity.sofd.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.enums.CustomerSetting;
import dk.digitalidentity.sofd.service.OS2SyncService;
import dk.digitalidentity.sofd.service.SettingService;

@Component
@EnableScheduling
public class OS2syncTask {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private SettingService settingService;
	
	@Autowired
	private OS2SyncService os2SyncService;

	// run once every minute
	@Scheduled(cron = "#{new java.util.Random().nextInt(60)} 0/1 * * * ?")
	public void processChanges() {
		if (configuration.getScheduled().isEnabled() && configuration.getIntegrations().getOs2sync().isEnabled()) {
			Long lastRun = settingService.getLongValueByKey(CustomerSetting.LAST_STSSYNC_RUN);

			if (lastRun > 0L) {
				os2SyncService.deltaUpdate(lastRun);
			}
			else {
				os2SyncService.fullUpdate();
			}
		}
	}
}