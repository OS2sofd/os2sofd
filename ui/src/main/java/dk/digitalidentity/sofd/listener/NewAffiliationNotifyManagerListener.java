package dk.digitalidentity.sofd.listener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.EmailTemplate;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import dk.digitalidentity.sofd.dao.model.EntityChangeQueueDetail;
import dk.digitalidentity.sofd.dao.model.OrgUnitManager;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplatePlaceholder;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplateType;
import dk.digitalidentity.sofd.listener.EntityListenerService.ChangeType;
import dk.digitalidentity.sofd.service.AffiliationService;
import dk.digitalidentity.sofd.service.EmailQueueService;
import dk.digitalidentity.sofd.service.EmailTemplateChildService;
import dk.digitalidentity.sofd.service.EmailTemplateService;
import dk.digitalidentity.sofd.service.PersonService;
import lombok.extern.slf4j.Slf4j;

// will trigger a welcome email being send to the employees manager whenever
// a new employee arrives in the organization. Will send the email immediately

@Component
@Slf4j
public class NewAffiliationNotifyManagerListener implements ListenerAdapter {

	@Autowired
	private PersonService personService;
	
	@Autowired
	private AffiliationService affiliationService;
	
	@Autowired
	private EmailTemplateService emailTemplateService;
	
	@Autowired
	private EmailQueueService emailQueueService;
	
	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired
	private EmailTemplateChildService emailTemplateChildService;
	
	@Override
	public void personCreated(String uuid) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return;
		}
		
		// if a new Person object is created, then ALL affiliations (from the wages system) is a new affiliation
		if (person.getAffiliations() != null) {
			List<Affiliation> affiliations = person.getAffiliations().stream()
					.filter(a -> a.getMaster().equals(configuration.getModules().getLos().getPrimeAffiliationMaster()))
					.collect(Collectors.toList());

			if (affiliations.size() > 0) {
				handleNewAffiliations(person, affiliations);
			}
		}
	}

	@Override
	public void personUpdated(String uuid, List<EntityChangeQueueDetail> changes) {
		List<EntityChangeQueueDetail> newEmployeeChanges = changes.stream().filter(c -> c.getChangeType().equals(ChangeType.ADDED_AFFILIATION)).collect(Collectors.toList());
		if (newEmployeeChanges.isEmpty()) {
			return;
		}

		Person person = personService.getByUuid(uuid);
		if (person == null) {
			log.warn("Could not find person with uuid: " + uuid);
			return;
		}

		List<Affiliation> affiliations = new ArrayList<>();
		for (EntityChangeQueueDetail newEmployeeChange : newEmployeeChanges) {
			Affiliation affiliation = affiliationService.findByUuid(newEmployeeChange.getChangeTypeDetails());
			if (affiliation == null) {
				log.warn("Unable to find affiliation with uuid: " + newEmployeeChange.getChangeTypeDetails());
				continue;
			}

			// only relevant for ACTUAL employments
			if (!affiliation.getMaster().equals(configuration.getModules().getLos().getPrimeAffiliationMaster())) {
				log.info("Skipping affilation with wrong master: " + affiliation.getUuid());
				continue;
			}

			affiliations.add(affiliation);
		}
		
		if (affiliations.size() > 0) {
			handleNewAffiliations(person, affiliations);
		}
	}

	private void handleNewAffiliations(Person person, List<Affiliation> affiliations) {
		EmailTemplate template = emailTemplateService.findByTemplateType(EmailTemplateType.NEW_AFFILIATION);

		for (EmailTemplateChild child : template.getChildren()) {
			if (!child.isEnabled()) {
				continue;
			}

			SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy");
			for (Affiliation affiliation : affiliations) {
				if( !emailTemplateService.shouldIncludeOrgUnit(child,affiliation.getCalculatedOrgUnit().getUuid()) ) {
					log.debug("Not sending email for email template child with id " + child.getId() + " for affiliation with uuid " + (affiliation != null ? affiliation.getUuid() : "<null>") + ". The affiliation OU was filtered out.");
					continue;
				}

				var managerResponse = PersonService.getManagerDifferentFromPerson(affiliation.getPerson(), affiliation);
				if (managerResponse == null) {
					log.warn("Unable to find manager for " + PersonService.getName(affiliation.getPerson()) + " / " + affiliation.getPerson().getUuid());
					continue;
				}

				Person manager = managerResponse.getManager();
				if (child.isOnlyManualRecipients()) {
					List<String> recipients = emailTemplateChildService.getRecipientsList(child.getRecipients());
					for( var recipient : recipients ) {
						sendNewAffiliationEmailOnlyManualRecipients(child, simpleDateFormat, affiliation, manager, recipient, managerResponse);
					}
				}
				else {
					List<Person> recipients = emailTemplateService.getManagerOrSubstitutes(child, manager, affiliation.getCalculatedOrgUnit().getUuid());
					for (Person recipient : recipients) {
						sendNewAffiliationEmail(child, simpleDateFormat, affiliation, manager, recipient, managerResponse);
					}
				}
			}
		}
	}

	private void sendNewAffiliationEmail(EmailTemplateChild child, SimpleDateFormat simpleDateFormat, Affiliation affiliation, Person manager, Person recipient, OrgUnitManager orgUnitManager) {
		String message = child.getMessage();
		message = message.replace(EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER.getPlaceholder(), PersonService.getName(affiliation.getPerson()));
		message = message.replace(EmailTemplatePlaceholder.AFFILIATIONUUID_PLACEHOLDER.getPlaceholder(), affiliation.getUuid());
		message = message.replace(EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER.getPlaceholder(), affiliation.getCalculatedOrgUnit().getName());
		message = message.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), PersonService.getName(manager));
		message = message.replace(EmailTemplatePlaceholder.TIMESTAMP_PLACEHOLDER.getPlaceholder(), simpleDateFormat.format(affiliation.getStartDate()));

		String title = child.getTitle();
		title = title.replace(EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER.getPlaceholder(), PersonService.getName(affiliation.getPerson()));
		title = title.replace(EmailTemplatePlaceholder.AFFILIATIONUUID_PLACEHOLDER.getPlaceholder(), affiliation.getUuid());
		title = title.replace(EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER.getPlaceholder(), affiliation.getCalculatedOrgUnit().getName());
		title = title.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), PersonService.getName(manager));
		title = title.replace(EmailTemplatePlaceholder.TIMESTAMP_PLACEHOLDER.getPlaceholder(), simpleDateFormat.format(affiliation.getStartDate()));

		var logContext = new StringBuilder();
		logContext.append("Skabelon: ").append(child.getTitle());
		logContext.append(", ").append("Medarbejder: ").append(PersonService.getName(affiliation.getPerson()));
		logContext.append(", ").append("Medarbejdernummer: ").append(affiliation.getEmployeeId());
		logContext.append(", ").append("Enhed: ").append(affiliation.getCalculatedOrgUnit().getName());

		emailQueueService.queueEmail(recipient, title, message, child.getMinutesDelay(), child, logContext.toString());
	}
	
	private void sendNewAffiliationEmailOnlyManualRecipients(EmailTemplateChild child, SimpleDateFormat simpleDateFormat, Affiliation affiliation, Person manager, String recipient, OrgUnitManager orgUnitManager) {
		String message = child.getMessage();
		message = message.replace(EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER.getPlaceholder(), PersonService.getName(affiliation.getPerson()));
		message = message.replace(EmailTemplatePlaceholder.AFFILIATIONUUID_PLACEHOLDER.getPlaceholder(), affiliation.getUuid());
		message = message.replace(EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER.getPlaceholder(), affiliation.getCalculatedOrgUnit().getName());
		message = message.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), PersonService.getName(manager));
		message = message.replace(EmailTemplatePlaceholder.TIMESTAMP_PLACEHOLDER.getPlaceholder(), simpleDateFormat.format(affiliation.getStartDate()));
		message = message.replace(EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER.getPlaceholder(), orgUnitManager.getOrgUnit().getName());

		String title = child.getTitle();
		title = title.replace(EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER.getPlaceholder(), PersonService.getName(affiliation.getPerson()));
		title = title.replace(EmailTemplatePlaceholder.AFFILIATIONUUID_PLACEHOLDER.getPlaceholder(), affiliation.getUuid());
		title = title.replace(EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER.getPlaceholder(), affiliation.getCalculatedOrgUnit().getName());
		title = title.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), PersonService.getName(manager));
		title = title.replace(EmailTemplatePlaceholder.TIMESTAMP_PLACEHOLDER.getPlaceholder(), simpleDateFormat.format(affiliation.getStartDate()));

		var logContext = new StringBuilder();
		logContext.append("Skabelon: ").append(child.getTitle());
		logContext.append(", ").append("Medarbejder: ").append(PersonService.getName(affiliation.getPerson()));
		logContext.append(", ").append("Medarbejdernummer: ").append(affiliation.getEmployeeId());
		logContext.append(", ").append("Enhed: ").append(affiliation.getCalculatedOrgUnit().getName());

		emailQueueService.queueEmail(recipient, title, message, child.getMinutesDelay(), child, logContext.toString());
	}
}
