package dk.digitalidentity.sofd.service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.EmailTemplate;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplatePlaceholder;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplateType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.OrgUnitChangeDao;
import dk.digitalidentity.sofd.dao.model.OrgUnitChange;
import dk.digitalidentity.sofd.dao.model.enums.OrgUnitChangeStatus;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@Slf4j
public class OrgUnitChangeService {
	@Autowired
	private OrgUnitChangeDao orgUnitChangeDao;

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private OrgUnitChangeService orgUnitChangeService;

	@Autowired
	private EmailTemplateService emailTemplateService;
	
	@Autowired
	private EmailTemplateChildService emailTemplateChildService;

	@Autowired
	private EmailQueueService emailQueueService;

	@Autowired
	private TemplateEngine templateEngine;

	// using already existing configuration although it breaks segregation
	@Value("${di.saml.sp.baseUrl}")
	private String linkBaseUrl;

	public OrgUnitChange save(OrgUnitChange orgUnitChange) {
		return orgUnitChangeDao.save(orgUnitChange);
	}
	
	public List<OrgUnitChange> getByStatus(OrgUnitChangeStatus status) {
		return orgUnitChangeDao.findByStatus(status);
	}
	
	public void deleteAll(List<OrgUnitChange> toDelete) {
		orgUnitChangeDao.deleteAll(toDelete);
	}

	@Transactional
	public void processOrgUnitChanges() {
		if (!configuration.getScheduled().isEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}

		List<OrgUnitChange> pendingChanges = orgUnitChangeService.getByStatus(OrgUnitChangeStatus.PENDING);
		if (pendingChanges.isEmpty()) {
			return;
		}
		pendingChanges.sort((a,b) -> a.getChangedTimestamp().compareTo(b.getChangedTimestamp()));
		String html = getHtmlString(pendingChanges);
		
		EmailTemplate template = emailTemplateService.findByTemplateType(EmailTemplateType.ORGUNIT_CHANGES);
		for (EmailTemplateChild child : template.getChildren()) {
			if (!child.isEnabled()) {
				continue;
			}

			List<String> recipients = emailTemplateChildService.getRecipientsList(child.getRecipients());
			if (recipients.isEmpty()) {
				continue;
			}
			
			for (String recipient : recipients) {
				String message = child.getMessage();
				message = message.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), recipient);
				message = message.replace(EmailTemplatePlaceholder.CHANGES_PLACEHOLDER.getPlaceholder(), html);
				
				String title = child.getTitle();
				title = title.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), recipient);
				title = title.replace(EmailTemplatePlaceholder.CHANGES_PLACEHOLDER.getPlaceholder(), html);

				var logContext = new StringBuilder();
				logContext.append("Skabelon: ").append(child.getTitle());
				emailQueueService.queueEmailToSystemMailbox(recipient, title, message, 0, child, logContext.toString());
			}
		}
		

		for (OrgUnitChange change : pendingChanges) {
			change.setStatus(OrgUnitChangeStatus.SENT);
			change.setSentTimestamp(new Date());
			orgUnitChangeService.save(change);
		}

		deleteOldSent();
	}

	private String getHtmlString(List<OrgUnitChange> pendingChanges) {
		Context context = new Context();
		context.setVariable("pendingChanges", pendingChanges);
		context.setVariable("linkBaseUrl", linkBaseUrl);
		String html = templateEngine.process("orgunit/changes/pendingChangesEmailTable", context);
		return html;
	}

	private void deleteOldSent() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -1);
		Date oneMonthAgo = cal.getTime();
		List<OrgUnitChange> toBeDeleted = orgUnitChangeService.getByStatus(OrgUnitChangeStatus.SENT).stream().filter(c -> c.getSentTimestamp().before(oneMonthAgo)).collect(Collectors.toList());
		orgUnitChangeService.deleteAll(toBeDeleted);
	}

}
