package dk.digitalidentity.sofd.listener;

import java.util.List;
import java.util.Objects;

import dk.digitalidentity.sofd.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.EmailTemplate;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import dk.digitalidentity.sofd.dao.model.EntityChangeQueueDetail;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplatePlaceholder;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplateType;
import dk.digitalidentity.sofd.listener.EntityListenerService.ChangeType;

@Component
public class NewSubstituteUserListener implements ListenerAdapter {

	@Autowired
	private EmailTemplateService emailTemplateService;
	
	@Autowired
	private EmailQueueService emailQueueService;
	
	@Autowired
	private PersonService personService;
	
	@Autowired
	private SofdConfiguration configuration;

	@Override
	public void personCreated(String uuid) {
		if (!configuration.getModules().getSubstitute().isEnabled()) {
			return;
		}
		
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return;
		}

		List<User> users = PersonService.getUsers(person);
		for (User user : users) {
			sendEboksMessage(person, user);
		}
	}
	
	@Override
	public void personUpdated(String uuid, List<EntityChangeQueueDetail> changes) {
		if (!configuration.getModules().getSubstitute().isEnabled()) {
			return;
		}

		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return;
		}

		// we are only interested in events regarding newly created users or reactivated users
		changes = changes.stream().filter(c -> c.getChangeType().equals(ChangeType.NEW_OR_REACTIVATED_USER)).toList();
		if (changes.isEmpty()) {
			return;
		}

		List<User> users = PersonService.getUsers(person);

		for (EntityChangeQueueDetail change : changes) {
			String[] tokens = change.getChangeTypeDetails().split(";");
			
			for (User user : users) {
				if (Objects.equals(tokens[0], user.getMaster()) && Objects.equals(tokens[1], user.getMasterId())) {
					sendEboksMessage(person, user);
					break;
				}
			}
		}
	}

	private void sendEboksMessage(Person person, User user) {
		// we only trigger on active vikar accounts (vikXXXX)
		if (!UserService.isSubstituteUser(user) || user.isDisabled()) {
			return;
		}

		EmailTemplate template = emailTemplateService.findByTemplateType(EmailTemplateType.AD_CREATE_SUBSTITUTE);
		for (EmailTemplateChild child : template.getChildren()) {
			if (child.isEnabled()) {
				String message = child.getMessage();
				message = message.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), PersonService.getName(person));
				message = message.replace(EmailTemplatePlaceholder.ACCOUNT_PLACEHOLDER.getPlaceholder(), user.getUserId());
				message = message.replace(EmailTemplatePlaceholder.TITLE.getPlaceholder(), child.getTitle());

				String title = child.getTitle();
				title = title.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), PersonService.getName(person));
				title = title.replace(EmailTemplatePlaceholder.ACCOUNT_PLACEHOLDER.getPlaceholder(), user.getUserId());
				title = title.replace(EmailTemplatePlaceholder.TITLE.getPlaceholder(), child.getTitle());

				long minutesDelay = 0;
				var logContext = new StringBuilder();
				logContext.append("Skabelon: ").append(child.getTitle());
				logContext.append(", ").append("Konto: ").append(user.getUserId());

				emailQueueService.queueEboks(person, title, message, minutesDelay, child, logContext.toString());
			}
		}
	}
}
