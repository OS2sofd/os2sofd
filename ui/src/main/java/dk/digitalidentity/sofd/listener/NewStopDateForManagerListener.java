package dk.digitalidentity.sofd.listener;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.EmailTemplate;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import dk.digitalidentity.sofd.dao.model.EntityChangeQueueDetail;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplateType;
import dk.digitalidentity.sofd.listener.EntityListenerService.ChangeType;
import dk.digitalidentity.sofd.service.EmailQueueService;
import dk.digitalidentity.sofd.service.EmailTemplateChildService;
import dk.digitalidentity.sofd.service.EmailTemplateService;
import dk.digitalidentity.sofd.service.PersonService;
import lombok.extern.slf4j.Slf4j;

// on stopDate change (affiliation), send mail if a managers stop date has changed from null to not null

@Slf4j
@Component
public class NewStopDateForManagerListener implements ListenerAdapter {

	@Autowired
	private PersonService personService;

	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired
	private EmailTemplateService emailTemplateService;
	
	@Autowired
	private EmailTemplateChildService emailTemplateChildService;
	
	@Autowired
	private EmailQueueService emailQueueService;

	@Override
	public void personUpdated(String uuid, List<EntityChangeQueueDetail> changes) {
		List<EntityChangeQueueDetail> affiliationChanges = changes.stream().filter(c -> c.getChangeType().equals(ChangeType.CHANGED_AFFILIATION_STOP_DATE)).collect(Collectors.toList());
		if (affiliationChanges.isEmpty()) {
			return;
		}

		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return;
		}
		
		for (EntityChangeQueueDetail change : affiliationChanges) {
			if ((change.getOldValue() == null && change.getNewValue() != null) || (change.getOldValue() != null && !change.getOldValue().equals(change.getNewValue()))) {
				Affiliation affiliation = person.getAffiliations().stream().filter(a -> configuration.getModules().getLos().getPrimeAffiliationMaster().equals(a.getMaster()) && Objects.equals(a.getEmployeeId(), change.getChangeTypeDetails())).findFirst().orElse(null);
				if (affiliation != null && !affiliation.getManagerFor().isEmpty()) {
					sendMail(person, affiliation);
				}
			}
		}
	}

	private void sendMail(Person person, Affiliation oAffiliation) {
		EmailTemplate template = emailTemplateService.findByTemplateType(EmailTemplateType.MANAGER_STOPS);
		for (EmailTemplateChild child : template.getChildren()) {
			if (!child.isEnabled()) {
				continue;
			}

			List<String> recipients = emailTemplateChildService.getRecipientsList(child.getRecipients());
			if (recipients.isEmpty()) {
				continue;
			}
			
			if (configuration.getEmailTemplate().isOrgFilterEnabled() && template.getTemplateType().isShowOrgFilter()) {
				List<String> excludedOUUuids = child.getExcludedOrgUnitMappings().stream().map(o -> o.getOrgUnit()).map(o -> o.getUuid()).collect(Collectors.toList());
				if (excludedOUUuids.contains(oAffiliation.getOrgUnit().getUuid())) {
					log.info("Not sending email for email template child with id " + child.getId() + " for affiliation with uuid " + oAffiliation.getUuid() + ". The affiliation OU was in the excluded ous list");
					continue;
				}
			}

			for (String recipient : recipients) {
				String message = child.getMessage();
				message = message.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, recipient);
				message = message.replace(EmailTemplateService.ORGUNIT_PLACEHOLDER, oAffiliation.getOrgUnit().getName());
				message = message.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, PersonService.getName(person));
				
				String title = child.getTitle();
				title = title.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, recipient);
				title = title.replace(EmailTemplateService.ORGUNIT_PLACEHOLDER, oAffiliation.getOrgUnit().getName());
				title = title.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, PersonService.getName(person));
				
				emailQueueService.queueEmail(recipient, title, message, 0, child);
			}
		}
	}
}
