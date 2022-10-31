package dk.digitalidentity.sofd.listener;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.EmailTemplate;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import dk.digitalidentity.sofd.dao.model.EntityChangeQueueDetail;
import dk.digitalidentity.sofd.dao.model.FunctionAssignment;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplateType;
import dk.digitalidentity.sofd.listener.EntityListenerService.ChangeType;
import dk.digitalidentity.sofd.service.EmailQueueService;
import dk.digitalidentity.sofd.service.EmailTemplateService;
import dk.digitalidentity.sofd.service.FunctionAssignmentService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SettingService;

@Component
public class OrgUnitManagerChangesListener implements ListenerAdapter {

	@Autowired
	private OrgUnitService orgUnitService;
	
	@Autowired
	private FunctionAssignmentService functionAssignmentService;
	
	@Autowired
	private SettingService settingService;
	
	@Autowired
	private PersonService personService;
	
	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private EmailTemplateService emailTemplateService;

	@Autowired
	private EmailQueueService emailQueueService;


	@Override
	public void orgUnitUpdated(String uuid, List<EntityChangeQueueDetail> changes) {
		if (!configuration.getModules().getFunctionHierarchy().isEnabled() || !settingService.getFunctionAssignmentEmployeeNewManager()) {
			return;
		}
		
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
			if (oldManagerMail == null && newManagerMail == null) {
				return;
			}

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

					if (oldManagerMail != null) {
						String message = child.getMessage();
						message = message.replace(EmailTemplateService.ORGUNIT_PLACEHOLDER, orgUnit.getName());
						message = message.replace(EmailTemplateService.FUNCTION_LIST, functions.toString());
						message = message.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, oldManagerName);

						String title = child.getTitle();
						title = title.replace(EmailTemplateService.ORGUNIT_PLACEHOLDER, orgUnit.getName());
						title = title.replace(EmailTemplateService.FUNCTION_LIST, functions.toString());
						title = title.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, oldManagerName);

						emailQueueService.queueEmail(oldManagerMail, title, message, 0, child);
					}
					if (newManagerMail != null) {
						String message = child.getMessage();
						message = message.replace(EmailTemplateService.ORGUNIT_PLACEHOLDER, orgUnit.getName());
						message = message.replace(EmailTemplateService.FUNCTION_LIST, functions.toString());
						message = message.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, newManagerName);

						String title = child.getTitle();
						title = title.replace(EmailTemplateService.ORGUNIT_PLACEHOLDER, orgUnit.getName());
						title = title.replace(EmailTemplateService.FUNCTION_LIST, functions.toString());
						title = title.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, newManagerName);

						emailQueueService.queueEmail(newManagerMail, title, message, 0, child);
					}
				}
			}
		}

		
	}
}
