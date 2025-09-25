package dk.digitalidentity.sofd.listener;

import dk.digitalidentity.sofd.config.SofdConfiguration;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class NewADUserListener implements ListenerAdapter {

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

		// we only want to send Eboks message if account creation is not enabled, otherwise it will be sent double
		if (configuration.getModules().getAccountCreation().isEnabled()) {
			return;
		}
		
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return;
		}

		List<User> users = PersonService.getUsers(person).stream().filter(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType())).toList();
		for (User user : users) {
			sendEboksMessage(person, user);
		}
	}
	
	@Override
	public void personUpdated(String uuid, List<EntityChangeQueueDetail> changes) {
		
		// we only want to send Eboks message if account creation is not enabled, otherwise it will be sent double
		if (configuration.getModules().getAccountCreation().isEnabled()) {
			return;
		}

		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return;
		}

		// we are only interested in events regarding newly created users
		changes = changes.stream().filter(c -> c.getChangeType().equals(ChangeType.NEW_AD_USER)).toList();
		if (changes.isEmpty()) {
			return;
		}

		List<User> users = PersonService.getUsers(person).stream().filter(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType())).toList();

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
		EmailTemplate template = emailTemplateService.findByTemplateType(EmailTemplateType.AD_CREATE_EMPLOYEE);
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

				var logContext = new StringBuilder();
				logContext.append("Skabelon: ").append(child.getTitle());
				logContext.append(", ").append("Konto: ").append(user.getUserId());

				emailQueueService.queueEboks(person, title, message, 0, child, logContext.toString());
			}
		}
	}
}
