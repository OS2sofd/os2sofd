package dk.digitalidentity.sofd.service;

import static dk.digitalidentity.sofd.util.NullChecker.getValue;

import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.EmailTemplate;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplatePlaceholder;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplateType;
import dk.digitalidentity.sofd.dao.model.enums.OrgUnitManagerSource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ManagerService {

	@Autowired
	private OrgUnitService orgUnitService;

	@Autowired
	private PersonService personService;

	@Autowired
	private EmailTemplateService emailTemplateService;

	@Autowired
	private EmailTemplateChildService emailTemplateChildService;

	@Autowired
	private EmailQueueService emailQueueService;

	@Transactional
	public void ensureValidManagers() {
		var allOrgUnits = orgUnitService.getAll();
		for( var orgUnit : allOrgUnits ) {
			try {
				// cleanup imported managers
				if(orgUnit.getImportedManagerUuid() != null ) {
					var validManager = getValidManager(orgUnit.getImportedManagerUuid());
					if( validManager == null )
					{
						var previousManager = orgUnit.getImportedManagerUuid();
						// manager is no longer valid, remove it
						log.info("Removing invalid imported manager " + previousManager + " from orgunit " + orgUnit.getName() + " (" + orgUnit.getUuid() + ")");
						orgUnit.setImportedManagerUuid(null);
						orgUnitService.save(orgUnit);
						// if orgunit has no selected manager, this change should trigger email templates
						if(orgUnit.getSelectedManagerUuid() != null ) {
							sendMail(orgUnit, EmailTemplateType.MANAGER_REMOVED, previousManager);
						}
					}
				}

				// cleanup selected managers
				if(orgUnit.getSelectedManagerUuid() != null ) {
					var validManager = getValidManager(orgUnit.getSelectedManagerUuid());
					if( validManager == null )
					{
						var previousManager = orgUnit.getSelectedManagerUuid();
						// manager is no longer valid, remove it
						log.info("Removing invalid selected manager " + previousManager + " from orgunit " + orgUnit.getName() + " (" + orgUnit.getUuid() + ")");
						orgUnit.setSelectedManagerUuid(null);
						// since we prefer selected over imported managers this removal should always trigger email templates
						sendMail(orgUnit, EmailTemplateType.MANAGER_REMOVED, previousManager);
					}
				}
			}
			catch(Exception e) {
				log.error("Failed to ensure valid manager for orgunit " + orgUnit.getName() + " (" + orgUnit.getUuid() + ")", e);
			}
		}
	}

	public record OrgUnitManagerDto(String orgunitUuid, String managerUuid ) { }
	public void importManagers(List<OrgUnitManagerDto> importManagers) {
		var allOrgUnits = orgUnitService.getAll();
		for( var orgUnit : allOrgUnits ) {
			try {
				var importedManagerUuid = importManagers.stream().filter(o -> o.orgunitUuid.equalsIgnoreCase(orgUnit.getUuid())).map(o -> o.managerUuid).findFirst().orElse(null);
				var validManager = importedManagerUuid != null ? getValidManager(importedManagerUuid) : null;
				var validManagerUuid = validManager != null ? validManager.getUuid() : null;
				if( !Objects.equals(orgUnit.getImportedManagerUuid(),validManagerUuid) ) {
					var previousManager = orgUnit.getImportedManagerUuid();
					orgUnit.setImportedManagerUuid(validManagerUuid);
					orgUnitService.save(orgUnit);
					// if orgUnit does not have a selected manager, this change should trigger email templates
					if( orgUnit.getSelectedManagerUuid() == null && !orgUnit.isDeleted() ) {
						if( orgUnit.getImportedManagerUuid() == null ) {
							// the manager was removed
							sendMail(orgUnit, EmailTemplateType.MANAGER_REMOVED, previousManager);
						}
						else
						{
							// the manager was changed or added
							sendMail(orgUnit, EmailTemplateType.NEW_MANAGER, PersonService.getName(validManager));
						}
					}
				}
			}
			catch (Exception e) {
				log.error("Failed to set manager on orgunit: " + orgUnit.getUuid(), e);
			}
		}
	}

	public boolean editSelectedManager(OrgUnit orgUnit, String managerUuid) throws Exception {
		log.trace("editSelectedManager, orgUnit: " + orgUnit + ", managerUuid: " + managerUuid);
		if( !StringUtils.hasLength(managerUuid)) {
			managerUuid = null;
		}
		var validManager = managerUuid != null ? getValidManager(managerUuid) : null;
		if( managerUuid != null && validManager == null ) {
			throw new Exception("Person with uuid " + managerUuid + " is not a valid manager");
		}
		var validManagerUuid = validManager != null ? validManager.getUuid() : null;
		log.trace("validManagerUuid: " + validManagerUuid);
		// the selected manager matches the imported manager - we just remove the previously manually selected one if it exists
		if( orgUnit.getManager() != null && orgUnit.getManager().getSource() == OrgUnitManagerSource.IMPORTED && Objects.equals(orgUnit.getManager().getManagerUuid(), validManagerUuid ))
		{
			if( orgUnit.getSelectedManagerUuid() != null ) {
				var previousManager = orgUnit.getSelectedManagerUuid();
				var previousManagerName = orgUnit.getManager().getName();
				log.trace("Clearing selected manager");
				orgUnit.setSelectedManagerUuid(null);
				if( !Objects.equals(previousManager,validManagerUuid) ) {
					// the manager was removed
					sendMail(orgUnit, EmailTemplateType.MANAGER_REMOVED, previousManagerName);
				}
				return true;
			}
		}
		// selected manager matches already selected manager (either directly or inherited)
		else if( orgUnit.getManager() != null && orgUnit.getManager().getSource() == OrgUnitManagerSource.SELECTED && Objects.equals(orgUnit.getManager().getManagerUuid(), validManagerUuid )) {
			// ignore and return no changes
			return false;
		}
		else if( !Objects.equals(orgUnit.getSelectedManagerUuid(), validManagerUuid ) ) {
			// this is a change and since we prefer selected over imported managers this change should always trigger email templates
			var previousManager = orgUnit.getManager();
			log.trace("Setting selected manager to " + validManagerUuid);
			orgUnit.setSelectedManagerUuid(validManagerUuid);
			if( orgUnit.getSelectedManagerUuid() == null && previousManager != null) {
				// the manager was removed
				sendMail(orgUnit, EmailTemplateType.MANAGER_REMOVED, previousManager.getName());
			}
			else
			{
				// the manager was changed or added
				sendMail(orgUnit, EmailTemplateType.NEW_MANAGER, PersonService.getName(validManager));
			}
			return true;
		}
		return false;
	}

	private Person getValidManager(String managerUuid) {
		var person = personService.getByUuid(managerUuid);
		var isValid = person != null
				&& !person.isForceStop()
				&& !person.isDead()
				&& !person.isDeleted()
				&& !person.isOnActiveLeave()
				&& person.getAffiliations().stream().anyMatch(Affiliation::isPrime);
		if( !isValid ) {
			log.warn("Invalid managerUuid: " + managerUuid );
			return null;
		}
		return person;
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
				if( orgUnit.getManager() != null ) {
					var managerOrSubstitutes = emailTemplateService.getManagerOrSubstitutes(child, orgUnit.getManager().getManager(), orgUnit.getUuid());
					for( var managerOrSubstitute : managerOrSubstitutes ) {
						var email = PersonService.getEmail(managerOrSubstitute);
						if( StringUtils.hasLength(email) ) {
							recipients.add(email);
						}
					}
				}
			}

			if (recipients.isEmpty()) {
				continue;
			}

			if( !emailTemplateService.shouldIncludeOrgUnit(child,orgUnit.getUuid()) ) {
				log.debug("Not sending email for email template child with id " + child.getId() + ". The OU was filtered out.");
				continue;
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

				var logContext = new StringBuilder();
				logContext.append("Skabelon: ").append(child.getTitle());
				logContext.append(", ").append("Enhed: ").append(orgUnit.getName());
				logContext.append(", ").append("Medarbejder: ").append(employeeName);
				emailQueueService.queueEmailToSystemMailbox(recipient, title, message, 0, child, logContext.toString());
			}
		}
	}
}
