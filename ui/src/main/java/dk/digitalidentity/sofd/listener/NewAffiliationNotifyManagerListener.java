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
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplateType;
import dk.digitalidentity.sofd.listener.EntityListenerService.ChangeType;
import dk.digitalidentity.sofd.service.AffiliationService;
import dk.digitalidentity.sofd.service.EmailQueueService;
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
				if (configuration.getEmailTemplate().isOrgFilterEnabled() && template.getTemplateType().isShowOrgFilter()) {
					List<String> excludedOUUuids = child.getExcludedOrgUnitMappings().stream().map(o -> o.getOrgUnit()).map(o -> o.getUuid()).collect(Collectors.toList());
					if (excludedOUUuids.contains(affiliation.getOrgUnit().getUuid())) {
						log.info("Not sending email for email template child with id " + child.getId() + " for affiliation with uuid " + affiliation.getUuid() + ". The affiliation OU was in the excluded ous list");
						continue;
					}
				}
				
				Person manager = PersonService.getManager(affiliation.getPerson(), affiliation.getEmployeeId());
				if (manager == null) {
					log.warn("Unable to find manager for " + PersonService.getName(affiliation.getPerson()) + " / " + affiliation.getPerson().getUuid());
					continue;
				}

				List<String> emailRecipients = emailTemplateService.getManagerOrSubstitutes(child, manager, affiliation.getOrgUnit().getUuid());
				for (String email : emailRecipients) {
					sendNewAffiliationEmail(child, simpleDateFormat, affiliation, manager, email);
				}
			}
		}
	}

	private void sendNewAffiliationEmail(EmailTemplateChild child, SimpleDateFormat simpleDateFormat, Affiliation affiliation, Person manager, String email) {
		String message = child.getMessage();
		message = message.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, PersonService.getName(affiliation.getPerson()));
		message = message.replace(EmailTemplateService.AFFILIATIONUUID_PLACEHOLDER, affiliation.getUuid());
		message = message.replace(EmailTemplateService.ORGUNIT_PLACEHOLDER, affiliation.getOrgUnit().getName());
		message = message.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, PersonService.getName(manager));
		message = message.replace(EmailTemplateService.TIMESTAMP_PLACEHOLDER, simpleDateFormat.format(affiliation.getStartDate()));

		String title = child.getTitle();
		title = title.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, PersonService.getName(affiliation.getPerson()));
		title = title.replace(EmailTemplateService.AFFILIATIONUUID_PLACEHOLDER, affiliation.getUuid());
		title = title.replace(EmailTemplateService.ORGUNIT_PLACEHOLDER, affiliation.getOrgUnit().getName());
		title = title.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, PersonService.getName(manager));
		title = title.replace(EmailTemplateService.TIMESTAMP_PLACEHOLDER, simpleDateFormat.format(affiliation.getStartDate()));
		
		emailQueueService.queueEmail(email, title, message, child.getMinutesDelay(), child);
	}
}
