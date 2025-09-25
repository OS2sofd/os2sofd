package dk.digitalidentity.sofd.listener;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.dao.model.EmailTemplate;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import dk.digitalidentity.sofd.dao.model.EntityChangeQueueDetail;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplatePlaceholder;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplateType;
import dk.digitalidentity.sofd.listener.EntityListenerService.ChangeType;
import dk.digitalidentity.sofd.service.EmailQueueService;
import dk.digitalidentity.sofd.service.EmailTemplateService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class ADUserReactivatedListener implements ListenerAdapter {
	private final EmailQueueService emailQueueService;
	private final EmailTemplateService emailTemplateService;
	private final PersonService personService;
	
	public void personUpdated(String uuid, List<EntityChangeQueueDetail> changes) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return;
		}
		
		List<EntityChangeQueueDetail> reactivatedChanges = changes.stream()
		    .filter(c -> c.getChangeType().equals(ChangeType.REACTIVATED_AD_USER))
		    .toList();
		    
		if (reactivatedChanges.isEmpty()) {
		    return;
		}

		for (EntityChangeQueueDetail change : reactivatedChanges) {
			var reactivatedUserId = change.getChangeTypeDetails();
			sendEboksMessage(person, reactivatedUserId);
		}
		
	}
	
	private void sendEboksMessage(Person person, String userId) {
		EmailTemplate template = emailTemplateService.findByTemplateType(EmailTemplateType.AD_REACTIVATED);
		for (EmailTemplateChild child : template.getChildren()) {
			if (child.isEnabled()) {
				String message = child.getMessage();
				message = message.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), PersonService.getName(person));
				message = message.replace(EmailTemplatePlaceholder.ACCOUNT_PLACEHOLDER.getPlaceholder(), userId);
				message = message.replace(EmailTemplatePlaceholder.TITLE.getPlaceholder(), child.getTitle());

				String title = child.getTitle();
				title = title.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), PersonService.getName(person));
				title = title.replace(EmailTemplatePlaceholder.ACCOUNT_PLACEHOLDER.getPlaceholder(), userId);
				title = title.replace(EmailTemplatePlaceholder.TITLE.getPlaceholder(), child.getTitle());

				var logContext = new StringBuilder();
				logContext.append("Skabelon: ").append(child.getTitle());
				logContext.append(", ").append("Konto: ").append(userId);

				emailQueueService.queueEboks(person, title, message, 0, child, logContext.toString());
			}
		}
	}

}
