package dk.digitalidentity.sofd.service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.sofd.dao.FunctionAssignmentDao;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.EmailTemplate;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import dk.digitalidentity.sofd.dao.model.FunctionAssignment;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplateType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FunctionAssignmentService {

	@Autowired
	private FunctionAssignmentDao functionAssignmentDao;
	
	@Autowired
	private SettingService settingService;

	@Autowired
	private EmailTemplateService emailTemplateService;

	@Autowired
	private EmailQueueService emailQueueService;
	
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
	
	private List<FunctionAssignment> getByStopDateEquals(Date stopDate) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(stopDate);
		
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		Date start = cal.getTime();
		
		cal.set(Calendar.HOUR_OF_DAY, 23);
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 59);
		Date stop = cal.getTime();
		
		return functionAssignmentDao.findByStopDateBetween(start, stop);
	}
	
	@Transactional
	public long generateFunctionAssignmentExpiringNotifications() {
		if (settingService.getDaysBeforeFunctionAssignmentExpires() != null) {
			Long days = Long.parseLong(settingService.getDaysBeforeFunctionAssignmentExpires());
			Date date = new Date();
			Calendar cal = Calendar.getInstance();
			cal.setTime(date);
			cal.add(Calendar.DAY_OF_MONTH, days.intValue());
			
			List<FunctionAssignment> stopDateBefore = getByStopDateEquals(cal.getTime());
			for (FunctionAssignment assignment : stopDateBefore) {
				String managerMail = assignment.getAffiliation().getOrgUnit().getManager() != null ? PersonService.getEmail(assignment.getAffiliation().getOrgUnit().getManager().getManager()) : null;
				String managerName = assignment.getAffiliation().getOrgUnit().getManager() != null ? PersonService.getName(assignment.getAffiliation().getOrgUnit().getManager().getManager()) : "";

				if (managerMail == null) {
					log.warn("FunctionAssignment with id " + assignment.getId() + " expires in " + days + ". Won't send mail because manager mail is null.");
					continue;
				}

				EmailTemplate template = emailTemplateService.findByTemplateType(EmailTemplateType.FUNCTION_ASSIGNMENT_EXPIRES);
				for (EmailTemplateChild child : template.getChildren()) {
					if (!child.isEnabled()) {
						continue;
					}

					String message = child.getMessage();
					message = message.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, managerName);
					message = message.replace(EmailTemplateService.DAYS_BEFORE_EVENT, "" + days);
					message = message.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, PersonService.getName(assignment.getAffiliation().getPerson()));
					message = message.replace(EmailTemplateService.FUNCTION_NAME, assignment.getFunction().getName());

					String title = child.getTitle();
					title = title.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, managerName);
					title = title.replace(EmailTemplateService.DAYS_BEFORE_EVENT, "" + days);
					title = title.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, PersonService.getName(assignment.getAffiliation().getPerson()));
					title = title.replace(EmailTemplateService.FUNCTION_NAME, assignment.getFunction().getName());

					emailQueueService.queueEmail(managerMail, title, message, 0, child);
				}
			}
		}
		return 0;
	}
}
