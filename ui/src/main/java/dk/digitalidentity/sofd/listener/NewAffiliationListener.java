package dk.digitalidentity.sofd.listener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.EmailTemplate;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import dk.digitalidentity.sofd.dao.model.EntityChangeQueueDetail;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SubstituteAssignment;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplateType;
import dk.digitalidentity.sofd.listener.EntityListenerService.ChangeType;
import dk.digitalidentity.sofd.service.AffiliationService;
import dk.digitalidentity.sofd.service.EmailQueueService;
import dk.digitalidentity.sofd.service.EmailTemplateService;
import dk.digitalidentity.sofd.service.PersonService;
import lombok.extern.slf4j.Slf4j;

// will trigger a welcome email being send the the manager AND the employee at the
// first workday, as well as a reminder on the 14th workday

@Slf4j
@Component
public class NewAffiliationListener implements ListenerAdapter {

	@Autowired
	private PersonService personService;

	@Autowired
	private EmailTemplateService emailTemplateService;
	
	@Autowired
	private AffiliationService affiliationService;

	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired
	private EmailQueueService emailQueueService;

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
			return;
		}

		List<Affiliation> affiliations = new ArrayList<>();
		for (EntityChangeQueueDetail newEmployeeChange : newEmployeeChanges) {
			Affiliation affiliation = affiliationService.findByUuid(newEmployeeChange.getChangeTypeDetails());
			if (affiliation == null) {
				continue;
			}

			// only relevant for ACTUAL employments
			if (!affiliation.getMaster().equals(configuration.getModules().getLos().getPrimeAffiliationMaster())) {
				continue;
			}

			affiliations.add(affiliation);
		}
		
		if (affiliations.size() > 0) {
			handleNewAffiliations(person, affiliations);
		}
	}

	private void handleNewAffiliations(Person person, List<Affiliation> affiliations) {
		for (Affiliation affiliation : affiliations) {
			Person manager = PersonService.getManager(person, affiliation.getEmployeeId());
			if (manager == null) {
				continue;
			}

			Calendar cal = Calendar.getInstance();
			cal.setTime(affiliation.getStartDate());
			cal.set(Calendar.HOUR_OF_DAY, 7);
			Date firstTts = cal.getTime();
			cal.add(Calendar.DATE, 14);
			Date secondTts = cal.getTime();
			
			EmailTemplate template = emailTemplateService.findByTemplateType(EmailTemplateType.NEW_EMPLOYEE_WELCOME);
			for (EmailTemplateChild child : template.getChildren()) {
				if (child.isEnabled()) {
					if (configuration.getEmailTemplate().isOrgFilterEnabled() && template.getTemplateType().isShowOrgFilter()) {
						List<String> excludedOUUuids = child.getExcludedOrgUnitMappings().stream().map(o -> o.getOrgUnit()).map(o -> o.getUuid()).collect(Collectors.toList());
						if (excludedOUUuids.contains(affiliation.getOrgUnit().getUuid())) {
							log.info("Not sending email for email template child with id " + child.getId() + " for affiliation with uuid " + affiliation.getUuid() + ". The affiliation OU was in the excluded ous list");
							continue;
						}
					}
					
					String message = child.getMessage();
					message = message.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, PersonService.getName(person));
					message = message.replace(EmailTemplateService.MANAGER_PLACEHOLDER, PersonService.getName(manager));
					
					String title = child.getTitle();
					title = title.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, PersonService.getName(person));
					title = title.replace(EmailTemplateService.MANAGER_PLACEHOLDER, PersonService.getName(manager));
					
					List<Person> emailRecipients = getManagerOrSubstitutes(child, manager, affiliation.getOrgUnit().getUuid());
					emailRecipients.add(person);
					
					emailQueueService.queueEmail(title, message, firstTts, child, emailRecipients);
				}
			}
			
			EmailTemplate templateReminder = emailTemplateService.findByTemplateType(EmailTemplateType.NEW_EMPLOYEE_REMINDER);
			for (EmailTemplateChild child : templateReminder.getChildren()) {
				if (child.isEnabled()) {
					if (configuration.getEmailTemplate().isOrgFilterEnabled() && template.getTemplateType().isShowOrgFilter()) {
						List<String> excludedOUUuids = child.getExcludedOrgUnitMappings().stream().map(o -> o.getOrgUnit()).map(o -> o.getUuid()).collect(Collectors.toList());
						if (excludedOUUuids.contains(affiliation.getOrgUnit().getUuid())) {
							log.info("Not sending email for email template child with id " + child.getId() + " for affiliation with uuid " + affiliation.getUuid() + ". The affiliation OU was in the excluded ous list");
							continue;
						}
					}
					
					String messageReminder = child.getMessage();
					messageReminder = messageReminder.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, PersonService.getName(person));
					messageReminder = messageReminder.replace(EmailTemplateService.MANAGER_PLACEHOLDER, PersonService.getName(manager));
					
					String title = child.getTitle();
					title = title.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, PersonService.getName(person));
					title = title.replace(EmailTemplateService.MANAGER_PLACEHOLDER, PersonService.getName(manager));
					
					List<Person> emailRecipients = getManagerOrSubstitutes(child, manager, affiliation.getOrgUnit().getUuid());
					emailRecipients.add(person);
					
					emailQueueService.queueEmail(title, messageReminder, secondTts, child, emailRecipients);
				}
			}
		}
	}

	// tweaked copy of the one in EmailTemplateService
	private List<Person> getManagerOrSubstitutes(EmailTemplateChild child, Person manager, String orgUnitUuid) {
		List<Person> emailRecipients = new ArrayList<>();

		if (child.isSendToSubstitute()) {
			for (SubstituteAssignment assignment : manager.getSubstitutes()) {
				// substitutes without an email address is not very interesting
				if (PersonService.getEmail(assignment.getSubstitute()) == null) {
					continue;
				}

				switch (assignment.getContext().getIdentifier()) {
					case "SOFD":
						if (CollectionUtils.isEmpty(assignment.getConstraintMappings()) || assignment.getConstraintMappings().stream().anyMatch(soum -> Objects.equals(orgUnitUuid, soum.getOrgUnit().getUuid()))) {
							emailRecipients.add(assignment.getSubstitute());
						}
	
						break;
					case "GLOBAL":
						emailRecipients.add(assignment.getSubstitute());
						break;
					default:
						break;
				}
			}
		}

		if (emailRecipients.isEmpty()) {
			emailRecipients.add(manager);
		}

		return emailRecipients;
	}
}
