package dk.digitalidentity.sofd.listener;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.EmailTemplate;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import dk.digitalidentity.sofd.dao.model.EntityChangeQueueDetail;
import dk.digitalidentity.sofd.dao.model.FunctionAssignment;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplatePlaceholder;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplateType;
import dk.digitalidentity.sofd.listener.EntityListenerService.ChangeType;
import dk.digitalidentity.sofd.service.EmailQueueService;
import dk.digitalidentity.sofd.service.EmailTemplateChildService;
import dk.digitalidentity.sofd.service.EmailTemplateService;
import dk.digitalidentity.sofd.service.FunctionAssignmentService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.PersonService;

@Component
public class OrgUnitManagerChangesListener implements ListenerAdapter {

	@Autowired
	private OrgUnitService orgUnitService;
	
	@Autowired
	private FunctionAssignmentService functionAssignmentService;
	
	@Autowired
	private PersonService personService;
	
	@Autowired
	private EmailTemplateService emailTemplateService;

	@Autowired
	private EmailQueueService emailQueueService;

	@Autowired
	private EmailTemplateChildService emailTemplateChildService;

	@Override
	public void orgUnitUpdated(String uuid, List<EntityChangeQueueDetail> changes) {
		List<EntityChangeQueueDetail> orgUnitManagerChanges = changes.stream().filter(c -> c.getChangeType().equals(ChangeType.CHANGED_MANAGER)).collect(Collectors.toList());
		for (EntityChangeQueueDetail change : orgUnitManagerChanges) {
			OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
			if (orgUnit == null) {
				return;
			}
			
			Person oldManager = personService.getByUuid(change.getOldValue());
			Person newManager = personService.getByUuid(change.getNewValue());
			
			if (oldManager == null && newManager == null) {
				return;
			}
			
			String oldManagerMail = oldManager == null ? null : PersonService.getEmail(oldManager);
			String newManagerMail = newManager == null ? null : PersonService.getEmail(newManager);

			StringBuilder functions = new StringBuilder();
			boolean sendMail = false;
			for (Affiliation affiliation : orgUnit.getAffiliations()) {
				List<FunctionAssignment> functionAssignments = functionAssignmentService.getByAffiliation(affiliation);

				if (functionAssignments.isEmpty()) {
					continue;
				}

				for (FunctionAssignment assignment : functionAssignments) {
					functions.append(PersonService.getName(affiliation.getPerson())).append(": ").append(assignment.getFunction().getName()).append("\n");
					sendMail = true;
				}

			}

			if (sendMail) {
				EmailTemplate template = emailTemplateService.findByTemplateType(EmailTemplateType.FUNCTION_NEW_MANAGER);
				String oldManagerName = oldManager == null ? null : PersonService.getName(oldManager);
				String newManagerName = newManager == null ? null : PersonService.getName(newManager);

				for (EmailTemplateChild child : template.getChildren()) {
					if (!child.isEnabled()) {
						continue;
					}

					String message = child.getMessage();
					message = message.replace(EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER.getPlaceholder(), orgUnit.getName());
					message = message.replace(EmailTemplatePlaceholder.FUNCTION_LIST.getPlaceholder(), functions.toString());

					String title = child.getTitle();
					title = title.replace(EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER.getPlaceholder(), orgUnit.getName());
					title = title.replace(EmailTemplatePlaceholder.FUNCTION_LIST.getPlaceholder(), functions.toString());

					var logContext = new StringBuilder();
					logContext.append("Skabelon: ").append(child.getTitle());
					logContext.append(", ").append("Enhed: ").append(orgUnit.getName());

					List<String> recipients = emailTemplateChildService.getRecipientsList(child.getRecipients());
					for( var recipient : recipients ) {
						var recipientMessage = message.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), recipient);
						var recipientTitle = title = title.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), recipient);
						emailQueueService.queueEmail(recipient, recipientTitle, recipientMessage, 0, child, logContext.toString() );
					}

					if( child.isOnlyManualRecipients()) {
						continue;
					}

					if (oldManagerMail != null) {
						var oldManagerMessage = message.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), oldManagerName);
						var oldManagerTitle = title = title.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), oldManagerName);
						emailQueueService.queueEmail(oldManager, oldManagerTitle, oldManagerMessage, 0, child, logContext.toString());
					}

					if (newManagerMail != null) {
						var newManagerMessage = message.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), newManagerName);
						var newManagerTitle = title.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), newManagerName);
						emailQueueService.queueEmail(newManager, newManagerTitle, newManagerMessage, 0, child, logContext.toString());
					}

				}
			}
		}

		
	}
}
