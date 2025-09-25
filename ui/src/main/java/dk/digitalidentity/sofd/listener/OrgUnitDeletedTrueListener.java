package dk.digitalidentity.sofd.listener;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.dao.model.EntityChangeQueueDetail;
import dk.digitalidentity.sofd.dao.model.Notification;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.NotificationType;
import dk.digitalidentity.sofd.listener.EntityListenerService.ChangeType;
import dk.digitalidentity.sofd.service.NotificationService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.SettingService;

@Component
public class OrgUnitDeletedTrueListener implements ListenerAdapter {

	@Autowired
	private OrgUnitService orgUnitService;
	
	@Autowired
	private SettingService settingService;
	
	@Autowired
	private NotificationService notificationService;

	@Override
	public void orgUnitUpdated(String uuid, List<EntityChangeQueueDetail> changes) {
		if (!settingService.isNotificationTypeEnabled(NotificationType.ORGUNIT_DELETED)) {
			return;
		}
		
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			return;
		}
		
		long orgUnitDeletedChanges = changes.stream().filter(c -> c.getChangeType().equals(ChangeType.DELETED_TRUE)).count();
		
		if (orgUnitDeletedChanges > 0) {
			Notification notification = new Notification();
			notification.setActive(true);
			notification.setAffectedEntityName(orgUnit.getName());
			notification.setAffectedEntityType(EntityType.ORGUNIT);
			notification.setAffectedEntityUuid(orgUnit.getUuid());
			notification.setCreated(new Date());
			notification.setNotificationType(NotificationType.ORGUNIT_DELETED);
			
			notificationService.save(notification);
		}
	}
}
