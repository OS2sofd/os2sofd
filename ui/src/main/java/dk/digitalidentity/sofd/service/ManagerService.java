package dk.digitalidentity.sofd.service;

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
	
	public boolean editManager(OrgUnit orgUnit, String managerUuid) {
		if (orgUnit.getManager() != null && !StringUtils.hasText(managerUuid)) {
			Person parentManager = orgUnit.getParent() != null && orgUnit.getParent().getManager() != null ? orgUnit.getParent().getManager().getManager() : null;

			String previousManager = orgUnit.getManager().getName();

			orgUnit.setManager(new OrgUnitManager(orgUnit,parentManager,true));
			for (OrgUnit child : orgUnit.getChildren()) {
				setManagerOnOrgUnitRecursive(child, parentManager);
			}

			sendMail(orgUnit, EmailTemplateType.MANAGER_REMOVED, previousManager);
			return true;
		} else if (managerWasAddedOrRemoved(orgUnit, managerUuid)) {
			Person person = personService.getByUuid(managerUuid);
			
			OrgUnitManager manager = new OrgUnitManager(orgUnit,person,false);
			orgUnit.setManager(manager);

			for (OrgUnit child : orgUnit.getChildren()) {
				setManagerOnOrgUnitRecursive(child, person);
			}

			sendMail(orgUnit, EmailTemplateType.NEW_MANAGER, orgUnit.getManager().getName());
			return true;
		}
		
		return false;
	}

	private void setManagerOnOrgUnitRecursive(OrgUnit orgUnit, Person person) {
		if (orgUnit.getManager() != null && orgUnit.getManager().isInherited() == false) {
			//break the recursion
			return;
		} else {
			//set manager
			if (person == null) {
				orgUnit.setManager(null);
				log.info("Removing Manager on " + orgUnit.getName());
			} else {
				OrgUnitManager manager = new OrgUnitManager(orgUnit,person,true);
				orgUnit.setManager(manager);
				log.info("Setting Manager on " + orgUnit.getName());
			}
		}

		for (OrgUnit child : orgUnit.getChildren()) {
			setManagerOnOrgUnitRecursive(child, person);
		}
	}

	private boolean managerWasAddedOrRemoved(OrgUnit orgUnit, String managerUuid) {
		//Add scenario
		if (orgUnit.getManager() == null && StringUtils.hasText(managerUuid)) {
			return true;
		} else
		//Modify scenario
		if (orgUnit.getManager() != null && !Objects.equals(orgUnit.getManager().getManager().getUuid(), managerUuid) ) {
			return true;
		}
		return false;
	}
	
	public void sendMail(OrgUnit orgUnit, EmailTemplateType type, String employeeName) {
		EmailTemplate template = emailTemplateService.findByTemplateType(type);
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
				if (excludedOUUuids.contains(orgUnit.getUuid())) {
					log.info("Not sending email for email template child with id " + child.getId() + " for orgUnit with uuid " + orgUnit.getUuid() + ". The OU was in the excluded ous list");
					continue;
				}
			}

			for (String recipient : recipients) {
				String message = child.getMessage();
				message = message.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, recipient);
				message = message.replace(EmailTemplateService.ORGUNIT_PLACEHOLDER, orgUnit.getName());
				message = message.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, employeeName);
				
				String title = child.getTitle();
				title = title.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, recipient);
				title = title.replace(EmailTemplateService.ORGUNIT_PLACEHOLDER, orgUnit.getName());
				title = title.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, employeeName);
				
				emailQueueService.queueEmail(recipient, title, message, 0, child);
			}
		}
	}
}
