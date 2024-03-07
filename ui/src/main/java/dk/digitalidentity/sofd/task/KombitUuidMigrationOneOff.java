package dk.digitalidentity.sofd.task;

import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.Setting;
import dk.digitalidentity.sofd.dao.model.enums.CustomerSetting;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SettingService;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class KombitUuidMigrationOneOff {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private SettingService settingService;
	
	@Autowired
	private PersonService personService;

	@EventListener(ApplicationReadyEvent.class)
	public void runOnStartup() {
		if (!configuration.getScheduled().isEnabled()) {
			return;
		}
		
		Setting setting = settingService.getByKey(CustomerSetting.COMPLETED_KOMBIT_UUID_MIGRATION);
		if (setting == null || Objects.equals(setting.getValue(), "NO")) {
			log.info("Running KombitUuidMigrationOneOff on startup");
			
			personService.migrateUuids();
			
			// flag job completed
			if (setting == null) {
				setting = new Setting();
				setting.setKey(CustomerSetting.COMPLETED_KOMBIT_UUID_MIGRATION.toString());
			}
			setting.setValue("YES");
			settingService.save(setting);
			
			log.info("Migration Done");
		}
	}
}
