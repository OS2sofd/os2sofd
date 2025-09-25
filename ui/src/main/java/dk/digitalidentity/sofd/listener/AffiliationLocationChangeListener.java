package dk.digitalidentity.sofd.listener;

import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.EmailTemplate;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import dk.digitalidentity.sofd.dao.model.EntityChangeQueueDetail;
import dk.digitalidentity.sofd.dao.model.Notification;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SubstituteAssignment;
import dk.digitalidentity.sofd.dao.model.SubstituteContext;
import dk.digitalidentity.sofd.dao.model.SubstituteOrgUnitAssignment;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplatePlaceholder;
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
import dk.digitalidentity.sofd.service.SubstituteOrgUnitAssignmentService;
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
	
	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private SubstituteOrgUnitAssignmentService substituteOrgUnitAssignmentService;

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
			
			// if this is an actual "wage" affiliation, we will create a notification - other masters we ignore
			if (Objects.equals(affiliation.getMaster(), configuration.getModules().getLos().getPrimeAffiliationMaster())) {
				createNotification(affiliation);
				createSubstituteWarningEmail(affiliation);
			}
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
			queueEmail(substituteReminder, substituteAssignment.getPerson(), substituteAssignment.getSubstitute(), substituteAssignment.getContext(), affiliation.getCalculatedOrgUnit());
		}

		List<SubstituteOrgUnitAssignment> substituteOrgUnitAssignments = substituteOrgUnitAssignmentService.findBySubstitute(affiliation.getPerson());
		for (SubstituteOrgUnitAssignment substituteAssignment : substituteOrgUnitAssignments) {
			String managerUuid = null;
			if( substituteAssignment.getOrgUnit().getManager() != null ) {
				managerUuid = substituteAssignment.getOrgUnit().getManager().getManagerUuid();
				queueEmail(substituteReminder, substituteAssignment.getOrgUnit().getManager().getManager(), substituteAssignment.getSubstitute(), substituteAssignment.getContext(),substituteAssignment.getOrgUnit().getManager().getOrgUnit());
			}


			// this means that if a substitute on an OrgUnit stops, and the context is isInheritOrgUnitAssignments, every manager under this ou will get notified
			if (substituteAssignment.getContext().isInheritOrgUnitAssignments()) {
				for (OrgUnit child : substituteAssignment.getOrgUnit().getChildren()) {

					handleQueueEmailRecursive(child, substituteReminder, substituteAssignment.getSubstitute(), substituteAssignment.getContext(),substituteAssignment.getOrgUnit(), managerUuid);
				}
			}
		}
	}

	private void handleQueueEmailRecursive(OrgUnit currentOU, EmailTemplate substituteReminder, Person substitute, SubstituteContext context, OrgUnit orgUnit, String previousManagerUuid) {
		String managerUuid = null;
		if( currentOU.getManager() != null) {
			managerUuid = currentOU.getManager().getManagerUuid();
			if( !Objects.equals(managerUuid,previousManagerUuid)) {
				queueEmail(substituteReminder, currentOU.getManager().getManager(), substitute, context, orgUnit);
			}
		}
		for (OrgUnit child : currentOU.getChildren()) {
			handleQueueEmailRecursive(child, substituteReminder, substitute, context, orgUnit, managerUuid);
		}
	}

	private void queueEmail(EmailTemplate substituteReminder, Person manager, Person substitute, SubstituteContext substituteContext, OrgUnit orgUnit) {
		String email = PersonService.getEmail(manager);
		if (!StringUtils.hasLength(email)) {
			log.warn("createSubstituteWarningEmail - no email address found for manager.");
			return;
		}

		for (EmailTemplateChild child : substituteReminder.getChildren()) {
			if (child.isEnabled()) {
				String message = child.getMessage();
				message = message.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), PersonService.getName(manager));
				message = message.replace(EmailTemplatePlaceholder.SUBSTITUTE_PLACEHOLDER.getPlaceholder(), PersonService.getName(substitute));
				message = message.replace(EmailTemplatePlaceholder.SUBSTITUTE_CONTEXT_PLACEHOLDER.getPlaceholder(), substituteContext.getName());
				message = message.replace(EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER.getPlaceholder(), orgUnit.getName());

				String title = child.getTitle();
				title = title.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), PersonService.getName(manager));
				title = title.replace(EmailTemplatePlaceholder.SUBSTITUTE_PLACEHOLDER.getPlaceholder(), PersonService.getName(substitute));
				title = title.replace(EmailTemplatePlaceholder.SUBSTITUTE_CONTEXT_PLACEHOLDER.getPlaceholder(), substituteContext.getName());
				title = title.replace(EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER.getPlaceholder(), orgUnit.getName());

				var logContext = new StringBuilder();
				logContext.append("Skabelon: ").append(child.getTitle());
				logContext.append(", ").append("Leder: ").append(PersonService.getName(manager));
				logContext.append(", ").append("Stedfortræder: ").append(PersonService.getName(substitute));
				logContext.append(", ").append("Stedfortræderkontekst: ").append(substituteContext.getName());
				logContext.append(", ").append("Enhed: ").append(orgUnit.getName());
				emailQueueService.queueEmail(manager, title, message, 0, child, logContext.toString());
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
		notification.setMessage("Nyt tilhørsforhold i " + affiliation.getCalculatedOrgUnit().getName() + " (" + affiliation.getCalculatedOrgUnit().getShortname() + ")");

		if (affiliation.getStartDate() != null) {
			notification.setEventDate(affiliation.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
		}
		
		notificationService.save(notification);
	}
}
