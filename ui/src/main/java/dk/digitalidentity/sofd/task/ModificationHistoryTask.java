package dk.digitalidentity.sofd.task;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.ModificationHistory;
import dk.digitalidentity.sofd.dao.model.Setting;
import dk.digitalidentity.sofd.dao.model.enums.CustomerSetting;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.service.SettingService;
import dk.digitalidentity.sofd.service.ModificationHistoryService;
import dk.digitalidentity.sofd.service.model.AuditWrapper;
import dk.digitalidentity.sofd.service.model.LatestModificationsWrapper;
import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class ModificationHistoryTask {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private ModificationHistoryService modificationHistoryService;

	@Autowired
	private SettingService settingService;

	@Scheduled(fixedRate = 1 * 60 * 1000)
	@Transactional(rollbackFor = Exception.class)
	public void processChanges() {
		if (!configuration.getScheduled().isEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}

		LatestModificationsWrapper latestModificationObject = modificationHistoryService.getLatestModifications();
		if (latestModificationObject != null && latestModificationObject.getLatestRevisionNumber() != null && latestModificationObject.getLatestRevisionNumber() > 0) {

			if (latestModificationObject.getOrgUnitChanges() != null && latestModificationObject.getOrgUnitChanges().size() > 0) {
				for (AuditWrapper ouChange : latestModificationObject.getOrgUnitChanges()) {
					ModificationHistory modificationHistory = new ModificationHistory();
					modificationHistory.setEntity(EntityType.ORGUNIT);
					modificationHistory.setUuid(ouChange.getUuid());
					modificationHistory.setChanged(new Date());
					modificationHistory.setChangeType(ouChange.getChangeType());

					modificationHistoryService.insert(modificationHistory);
				}
			}

			if (latestModificationObject.getPersonChanges() != null && latestModificationObject.getPersonChanges().size() > 0) {
				for (AuditWrapper personChange : latestModificationObject.getPersonChanges()) {
					ModificationHistory modificationHistory = new ModificationHistory();
					modificationHistory.setEntity(EntityType.PERSON);
					modificationHistory.setUuid(personChange.getUuid());
					modificationHistory.setChanged(new Date());
					modificationHistory.setChangeType(personChange.getChangeType());

					modificationHistoryService.insert(modificationHistory);
				}
			}

			updateLatestRevisionNumber(latestModificationObject.getLatestRevisionNumber());
		}
	}

	private void updateLatestRevisionNumber(Long revNumber) {
		Setting setting = settingService.getByKey(CustomerSetting.LAST_READ_REVISION);
		setting.setValue(Long.toString(revNumber));
		settingService.save(setting);
	}
}
