package dk.digitalidentity.sofd.service;

import static dk.digitalidentity.sofd.util.NullChecker.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.EmailTemplate;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.OrgUnitManager;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplatePlaceholder;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplateType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ManagerService {

	@Autowired
	private PersonService personService;

	@Autowired
	private EmailTemplateService emailTemplateService;

	@Autowired
	private EmailTemplateChildService emailTemplateChildService;

	@Autowired
	private EmailQueueService emailQueueService;

	@Autowired
	private SofdConfiguration configuration;

	public void checkAndSetManager(OrgUnit orgUnit) {
		Person parentManager = orgUnit.getParent() != null && orgUnit.getParent().getManager() != null ? orgUnit.getParent().getManager().getManager() : null;
		setManagerOnOrgUnitRecursive(orgUnit, parentManager);
	}

	public boolean editManager(OrgUnit orgUnit, String managerUuid) {

		if (orgUnit.getManager() != null && !StringUtils.hasText(managerUuid)) {
			Person parentManager = orgUnit.getParent() != null && orgUnit.getParent().getManager() != null ? orgUnit.getParent().getManager().getManager() : null;

			String previousManager = orgUnit.getManager().getName();

			if (parentManager != null) {
				orgUnit.setManager(new OrgUnitManager(orgUnit, parentManager, true));
			}
			else {
				orgUnit.setManager(null);
			}

			for (OrgUnit child : orgUnit.getChildren()) {
				setManagerOnOrgUnitRecursive(child, parentManager);
			}

			sendMail(orgUnit, EmailTemplateType.MANAGER_REMOVED, previousManager);

			return true;
		}
		else if (managerWasAddedOrRemoved(orgUnit, managerUuid)) {
			Person person = personService.getByUuid(managerUuid);

			OrgUnitManager manager = new OrgUnitManager(orgUnit, person, false);
			orgUnit.setManager(manager);

			for (OrgUnit child : orgUnit.getChildren()) {
				setManagerOnOrgUnitRecursive(child, person);
			}

			sendMail(orgUnit, EmailTemplateType.NEW_MANAGER, PersonService.getName(person));

			return true;
		}

		return false;
	}

	private void setManagerOnOrgUnitRecursive(OrgUnit orgUnit, Person person) {

		if (orgUnit.getManager() != null && orgUnit.getManager().isInherited() == false) {
			// break the recursion
			return;
		}
		else {

			// set manager
			if (person == null) {
				orgUnit.setManager(null);
				log.info("Removing Manager on " + orgUnit.getName());
			}
			else {
				OrgUnitManager manager = new OrgUnitManager(orgUnit, person, true);
				orgUnit.setManager(manager);
				log.info("Setting Manager on " + orgUnit.getName());
			}
		}

		if( orgUnit.getChildren() != null ) {
			for (OrgUnit child : orgUnit.getChildren()) {
				setManagerOnOrgUnitRecursive(child, person);
			}
		}
	}

	private boolean managerWasAddedOrRemoved(OrgUnit orgUnit, String managerUuid) {

		// Add scenario
		if (orgUnit.getManager() == null && StringUtils.hasText(managerUuid)) {
			return true;
		}
		// Modify scenario
		else if (orgUnit.getManager() != null && !Objects.equals(orgUnit.getManager().getManager().getUuid(), managerUuid)) {
			return true;
		}

		return false;
	}

	private String getSubstituteReplacementString(OrgUnit orgUnit) {
		StringBuilder substitutes = new StringBuilder();

		for (var substitute : orgUnit.getSubstitutes()) {
			substitutes.append(orgUnit.getName()).append(": ").append(PersonService.getName(substitute.getSubstitute())) .append(" (").append(substitute.getContext().getName()).append(")\n<br/>");
		}
		
		for (var child : orgUnit.getChildren()) {
			// only include children that has the same manager
			if (Objects.equals(getValue(() -> orgUnit.getManager().getManager().getUuid()), getValue(() -> child.getManager().getManager().getUuid()))) {
				substitutes.append(getSubstituteReplacementString(child));
			}
		}

		return substitutes.toString();
	}
	
	public void sendMail(OrgUnit orgUnit, EmailTemplateType type, String employeeName) {
		EmailTemplate template = emailTemplateService.findByTemplateType(type);

		var substituteReplacementString = getSubstituteReplacementString(orgUnit);

		for (EmailTemplateChild child : template.getChildren()) {

			if (!child.isEnabled()) {
				continue;
			}

			List<String> recipients = emailTemplateChildService.getRecipientsList(child.getRecipients());
			recipients.addAll(emailTemplateChildService.getRecipientsList(child.getRecipientsCC()));
			recipients.addAll(emailTemplateChildService.getRecipientsList(child.getRecipientsBCC()));

			if( !child.isOnlyManualRecipients())
			{
				var managerOrSubstitutes = emailTemplateService.getManagerOrSubstitutes(child, orgUnit.getManager().getManager(), orgUnit.getUuid());
				for( var managerOrSubstitute : managerOrSubstitutes ) {
					var email = PersonService.getEmail(managerOrSubstitute);
					if( StringUtils.hasLength(email) ) {
						recipients.add(email);
					}
				}
			}

			if (recipients.isEmpty()) {
				continue;
			}

			if (configuration.getEmailTemplate().isOrgFilterEnabled() && template.getTemplateType().isShowOrgFilter()) {
				List<String> excludedOUUuids = child.getExcludedOrgUnitMappings().stream().map(o -> o.getOrgUnit()).map(o -> o.getUuid()).collect(Collectors.toList());

				if (excludedOUUuids.contains(orgUnit.getUuid())) {
					log.info("Not sending email for email template child with id " + child.getId() + " for orgUnit with uuid " + orgUnit.getUuid() + ". The OU was in the excluded ous list");
					continue;
				}
			}

			for (String recipient : recipients) {
				String message = child.getMessage();
				message = message.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), recipient);
				message = message.replace(EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER.getPlaceholder(), orgUnit.getName());
				message = message.replace(EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER.getPlaceholder(), employeeName);
				message = message.replace(EmailTemplatePlaceholder.SUBSTITUTE_LIST.getPlaceholder(), substituteReplacementString);

				String title = child.getTitle();
				title = title.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), recipient);
				title = title.replace(EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER.getPlaceholder(), orgUnit.getName());
				title = title.replace(EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER.getPlaceholder(), employeeName);

				emailQueueService.queueEmailToSystemMailbox(recipient, title, message, 0, child);
			}
		}
	}
}
