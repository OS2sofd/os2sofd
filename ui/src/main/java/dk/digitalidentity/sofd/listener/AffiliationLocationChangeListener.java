package dk.digitalidentity.sofd.listener;

import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.EmailTemplate;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import dk.digitalidentity.sofd.dao.model.EntityChangeQueueDetail;
import dk.digitalidentity.sofd.dao.model.Notification;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SubstituteAssignment;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplateType;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.NotificationType;
import dk.digitalidentity.sofd.listener.EntityListenerService.ChangeType;
import dk.digitalidentity.sofd.service.AffiliationService;
import dk.digitalidentity.sofd.service.EmailQueueService;
import dk.digitalidentity.sofd.service.EmailTemplateService;
import dk.digitalidentity.sofd.service.NotificationService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SubstituteAssignmentService;
import lombok.extern.slf4j.Slf4j;

// will trigger the creation of a notification when an existing employee changes "work-place"
// will also trigger a substitute email warning if relevant

@Slf4j
@Component
public class AffiliationLocationChangeListener implements ListenerAdapter {

	@Autowired
	private PersonService personService;
	
	@Autowired
	private AffiliationService affiliationService;
	
	@Autowired
	private NotificationService notificationService;
	
	@Autowired
	private EmailTemplateService emailTemplateService;
	
	@Autowired
	private EmailQueueService emailQueueService;
	
	@Autowired
	private SubstituteAssignmentService substituteAssignmentService;

	@Override
	public void personUpdated(String uuid, List<EntityChangeQueueDetail> changes) {
		List<EntityChangeQueueDetail> affiliationChanges = changes.stream().filter(c -> c.getChangeType().equals(ChangeType.CHANGED_AFFILIATION_LOCATION)).collect(Collectors.toList());
		if (affiliationChanges.isEmpty()) {
			return;
		}

		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return;
		}
		
		for (EntityChangeQueueDetail affiliationChange : affiliationChanges) {
			Affiliation affiliation = affiliationService.findByUuid(affiliationChange.getChangeTypeDetails());
			if (affiliation == null) {
				continue;
			}

			createNotification(affiliation);
			createSubstituteWarningEmail(affiliation);
		}
	}

	private void createSubstituteWarningEmail(Affiliation affiliation) {
		EmailTemplate substituteReminder = emailTemplateService.findByTemplateType(EmailTemplateType.SUBSTITUTE_STOPS);
		
		boolean foundTemplate = false;
		for (EmailTemplateChild child : substituteReminder.getChildren()) {
			if (child.isEnabled()) {
				foundTemplate = true;
			}
		}

		if (!foundTemplate) {
			return;
		}

		List<SubstituteAssignment> substituteAssignments = substituteAssignmentService.findBySubstitute(affiliation.getPerson());
		for (SubstituteAssignment substituteAssignment : substituteAssignments) {
			Person manager = substituteAssignment.getPerson();
			Person substitute = substituteAssignment.getSubstitute();
	
			String email = PersonService.getEmail(manager);
			if (!StringUtils.hasLength(email)) {
				log.warn("createSubstituteWarningEmail - no email address found for manager.");
				continue;
			}
	
			for (EmailTemplateChild child : substituteReminder.getChildren()) {
				if (child.isEnabled()) {
					String message = child.getMessage();
					message = message.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, PersonService.getName(manager));
					message = message.replace(EmailTemplateService.SUBSTITUTE_PLACEHOLDER, PersonService.getName(substitute));
					message = message.replace(EmailTemplateService.SUBSTITUTE_CONTEXT_PLACEHOLDER, substituteAssignment.getContext().getName());
					
					String title = child.getTitle();
					title = title.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, PersonService.getName(manager));
					title = title.replace(EmailTemplateService.SUBSTITUTE_PLACEHOLDER, PersonService.getName(substitute));
					title = title.replace(EmailTemplateService.SUBSTITUTE_CONTEXT_PLACEHOLDER, substituteAssignment.getContext().getName());
					
					emailQueueService.queueEmail(email, title, message, 0, child);
				}
			}
		}
	}

	private void createNotification(Affiliation affiliation) {	
		Notification notification = new Notification();
		notification.setActive(true);
		notification.setAffectedEntityUuid(affiliation.getPerson().getUuid());
		notification.setAffectedEntityType(EntityType.PERSON);
		notification.setAffectedEntityName(PersonService.getName(affiliation.getPerson()));
		notification.setNotificationType(NotificationType.NEW_AFFILIATION_LOCATION);
		notification.setCreated(new Date());
		notification.setMessage("Nyt tilhørsforhold i " + affiliation.getOrgUnit().getName() + " (" + affiliation.getOrgUnit().getShortname() + ")");

		if (affiliation.getStartDate() != null) {
			notification.setEventDate(affiliation.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
		}
		
		notificationService.save(notification);
	}
}
