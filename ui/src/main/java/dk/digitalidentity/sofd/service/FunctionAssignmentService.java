package dk.digitalidentity.sofd.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import dk.digitalidentity.sofd.dao.FunctionAssignmentDao;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.EmailTemplate;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import dk.digitalidentity.sofd.dao.model.FunctionAssignment;
import dk.digitalidentity.sofd.dao.model.OrgUnitManager;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplatePlaceholder;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplateType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FunctionAssignmentService {

	@Autowired
	private FunctionAssignmentDao functionAssignmentDao;
	
	@Autowired
	private EmailTemplateService emailTemplateService;

	@Autowired
	private EmailQueueService emailQueueService;

	@Autowired
	private EmailTemplateChildService emailTemplateChildService;
	
	public List<FunctionAssignment> getAll() {
		return functionAssignmentDao.findAll();
	}

	public FunctionAssignment getById(long id) {
		return functionAssignmentDao.findById(id);
	}
	
	public List<FunctionAssignment> getByAffiliation(Affiliation affiliation) {
		return functionAssignmentDao.findByAffiliation(affiliation);
	}

	public void delete(FunctionAssignment functionAssignment) {
		functionAssignmentDao.delete(functionAssignment);
	}

	public FunctionAssignment save(FunctionAssignment functionAssignment) {
		return functionAssignmentDao.save(functionAssignment);
	}
	@Transactional
	public void generateFunctionAssignmentExpiringNotifications() {

		EmailTemplate template = emailTemplateService.findByTemplateType(EmailTemplateType.FUNCTION_ASSIGNMENT_EXPIRES);
		for (EmailTemplateChild child : template.getChildren()) {
			if (!child.isEnabled()) {
				continue;
			}
			LocalDate futureDate = LocalDate.now().plusDays(child.getDaysBeforeEvent());

			List<FunctionAssignment> expiringAssignments = functionAssignmentDao.findByStopDate(futureDate);
			for (FunctionAssignment assignment : expiringAssignments) {
				OrgUnitManager orgUnitManager = assignment.getAffiliation().getCalculatedOrgUnit().getManager();
				String managerMail = orgUnitManager != null ? PersonService.getEmail(orgUnitManager.getManager()) : null;
				String managerName = orgUnitManager != null ? PersonService.getName(orgUnitManager.getManager()) : "";

				String message = child.getMessage();
				message = message.replace(EmailTemplatePlaceholder.DAYS_BEFORE_EVENT.getPlaceholder(), "" + child.getDaysBeforeEvent());
				message = message.replace(EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER.getPlaceholder(), PersonService.getName(assignment.getAffiliation().getPerson()));
				message = message.replace(EmailTemplatePlaceholder.FUNCTION_NAME.getPlaceholder(), assignment.getFunction().getName());
				message = message.replace(EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER.getPlaceholder(), assignment.getAffiliation().getCalculatedOrgUnit().getName());

				String title = child.getTitle();
				title = title.replace(EmailTemplatePlaceholder.DAYS_BEFORE_EVENT.getPlaceholder(), "" + child.getDaysBeforeEvent());
				title = title.replace(EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER.getPlaceholder(), PersonService.getName(assignment.getAffiliation().getPerson()));
				title = title.replace(EmailTemplatePlaceholder.FUNCTION_NAME.getPlaceholder(), assignment.getFunction().getName());
				title = title.replace(EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER.getPlaceholder(), assignment.getAffiliation().getCalculatedOrgUnit().getName());

				var logContext = new StringBuilder();
				logContext.append("Skabelon: ").append(child.getTitle());
				logContext.append(", ").append("Medarbejder: ").append(PersonService.getName(assignment.getAffiliation().getPerson()));
				logContext.append(", ").append("Medarbejdernummer: ").append(assignment.getAffiliation().getEmployeeId());
				logContext.append(", ").append("Funktion: ").append(assignment.getFunction().getName());
				logContext.append(", ").append("Enhed: ").append(assignment.getAffiliation().getCalculatedOrgUnit().getName());

				List<String> manualRecipients = emailTemplateChildService.getRecipientsList(child.getRecipients());
				for (var recipient : manualRecipients) {
					if (StringUtils.hasLength(recipient)) {
						var recipientMessage = message.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), recipient);
						var recipientTitle = title.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), recipient);
						emailQueueService.queueEmail(recipient, recipientTitle, recipientMessage, 0, child, logContext.toString());
					}
				}

				if (!child.isOnlyManualRecipients()) {
					if (managerMail == null) {
						log.warn("FunctionAssignment with id " + assignment.getId() + " expires in " + child.getDaysBeforeEvent() + " days. Won't send mail because manager mail is null.");
						continue;
					}
					else {
						var managerMessage = message.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), managerName);
						var managerTitle = title.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), managerName);
						emailQueueService.queueEmail(orgUnitManager.getManager(), managerTitle, managerMessage, 0, child, logContext.toString());
					}
				}
			}
		}
	}

	@Transactional
	public void generateFunctionAssignmentFollowUpNotifications() {

		EmailTemplate template = emailTemplateService.findByTemplateType(EmailTemplateType.FUNCTION_ASSIGNMENT_FOLLOW_UP);
		for (EmailTemplateChild child : template.getChildren()) {
			if (!child.isEnabled()) {
				continue;
			}
			List<FunctionAssignment> followUpAssignments = functionAssignmentDao.getFollowUpAssignments();

			for (FunctionAssignment assignment : followUpAssignments) {
				OrgUnitManager orgUnitManager = assignment.getAffiliation().getCalculatedOrgUnit().getManager();
				String managerMail = orgUnitManager != null ? PersonService.getEmail(orgUnitManager.getManager()) : null;
				String managerName = orgUnitManager != null ? PersonService.getName(orgUnitManager.getManager()) : "";

				String message = child.getMessage();
				message = message.replace(EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER.getPlaceholder(), PersonService.getName(assignment.getAffiliation().getPerson()));
				message = message.replace(EmailTemplatePlaceholder.FUNCTION_NAME.getPlaceholder(), assignment.getFunction().getName());
				message = message.replace(EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER.getPlaceholder(), assignment.getAffiliation().getCalculatedOrgUnit().getName());

				String title = child.getTitle();
				title = title.replace(EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER.getPlaceholder(), PersonService.getName(assignment.getAffiliation().getPerson()));
				title = title.replace(EmailTemplatePlaceholder.FUNCTION_NAME.getPlaceholder(), assignment.getFunction().getName());
				title = title.replace(EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER.getPlaceholder(), assignment.getAffiliation().getCalculatedOrgUnit().getName());

				var logContext = new StringBuilder();
				logContext.append("Skabelon: ").append(child.getTitle());
				logContext.append(", ").append("Medarbejder: ").append(PersonService.getName(assignment.getAffiliation().getPerson()));
				logContext.append(", ").append("Medarbejdernummer: ").append(assignment.getAffiliation().getEmployeeId());
				logContext.append(", ").append("Funktion: ").append(assignment.getFunction().getName());
				logContext.append(", ").append("Enhed: ").append(assignment.getAffiliation().getCalculatedOrgUnit().getName());

				List<String> recipients = emailTemplateChildService.getRecipientsList(child.getRecipients());
				for( var recipient : recipients ) {
					var recipientMessage = message.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), recipient);
					var recipientTitle = title.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), recipient);
					emailQueueService.queueEmail(recipient, recipientTitle, recipientMessage, 0, child, logContext.toString());
				}

				if( !child.isOnlyManualRecipients()) {
					if (managerMail != null) {
						var managerMessage = message.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), managerName);
						var managerTitle = title.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), managerName);
						emailQueueService.queueEmail(orgUnitManager.getManager(), managerTitle, managerMessage, 0, child, logContext.toString());
					}
				}
			}

		}
	}

}
