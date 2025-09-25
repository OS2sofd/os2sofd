package dk.digitalidentity.sofd.listener;

import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.EntityChangeQueueDetail;
import dk.digitalidentity.sofd.dao.model.Notification;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.NotificationType;
import dk.digitalidentity.sofd.listener.EntityListenerService.ChangeType;
import dk.digitalidentity.sofd.service.AffiliationService;
import dk.digitalidentity.sofd.service.NotificationService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NewAffiliationInEmptyOrgUnitListener implements ListenerAdapter {
	private final AffiliationService affiliationService;
	private final NotificationService notificationService;
	private final PersonService personService;
	private final SettingService settingService;
	
	public void personUpdated(String uuid, List<EntityChangeQueueDetail> changes) {
		if (!settingService.isNotificationTypeEnabled(NotificationType.NEW_AFFILIATION_IN_EMPTY_ORGUNIT)) {
			return;
		}
		
		List<EntityChangeQueueDetail> newAffiliationInEmptyOrgUnitChanges = changes.stream().filter(c -> c.getChangeType().equals(ChangeType.NEW_AFFILIATION_IN_EMPTY_ORGUNIT)).collect(Collectors.toList());
		if (newAffiliationInEmptyOrgUnitChanges.isEmpty()) {
			return;
		}
		
		Person person = personService.getByUuid(uuid);
		
		for (EntityChangeQueueDetail newAffiliationInEmptyOrgUnitChange : newAffiliationInEmptyOrgUnitChanges) {
			Affiliation affiliation = affiliationService.findByUuid(newAffiliationInEmptyOrgUnitChange.getChangeTypeDetails());
			if (affiliation == null) {
				log.warn("Could not find affiliation with uuid: {}", newAffiliationInEmptyOrgUnitChange.getChangeTypeDetails());
				continue;
			}
			if (person == null) {
				person = affiliation.getPerson();
				if (person == null) {
					continue;
				}
			}
			
			Notification notification = new Notification();
			notification.setActive(true);
			notification.setAffectedEntityName(affiliation.getCalculatedOrgUnit().getName());
			notification.setAffectedEntityType(EntityType.ORGUNIT);
			notification.setAffectedEntityUuid(affiliation.getCalculatedOrgUnit().getUuid());
			notification.setCreated(new Date());
			notification.setNotificationType(NotificationType.NEW_AFFILIATION_IN_EMPTY_ORGUNIT);
			notification.setMessage("Tilhørsforhold " + AffiliationService.getPositionName(affiliation) + " (" + PersonService.getName(affiliation.getPerson()) + ")" + " oprettet i den tidligere tomme enhed " + affiliation.getCalculatedOrgUnit().getName() + " (" + affiliation.getCalculatedOrgUnit().getUuid() + ").");
			
			notificationService.save(notification);
		}
	}
}
