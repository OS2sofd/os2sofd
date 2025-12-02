package dk.digitalidentity.sofd.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.EmailService;
import dk.digitalidentity.sofd.service.NotificationService;
import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class NotificationGenerationTask {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private NotificationService notificationService;

	@Autowired
	private EmailService emailService;
	
	@Scheduled(cron = "${cron.notification.task:0 15 4 * * ?}")
	public void processChanges() {
		if (!configuration.getScheduled().isEnabled()) {
			return;
		}
		
		log.info("Starting notification generation");

		int count = 0;
		if (configuration.getModules().getAccountCreation().isEnabled()) {
			var missingRulesNotifications = notificationService.generateMissingRulesNotifications();
			log.info("missingRulesNotifications count: " + missingRulesNotifications);
			count += missingRulesNotifications;

			var missingRulesNotificationsTitles = notificationService.generateMissingRulesTitlesNotifications();
			log.info("missingRulesNotificationsTitles count: " + missingRulesNotificationsTitles);
			count += missingRulesNotificationsTitles;

			var usersNotSupportedByRuleNotifications = notificationService.generateUsersNotSupportedByRuleNotifications();
			log.info("usersNotSupportedByRuleNotifications count: " + usersNotSupportedByRuleNotifications);
			count += usersNotSupportedByRuleNotifications;
		}

		if (count > 0 && StringUtils.hasLength(configuration.getCustomer().getNotificationEmail())) {
			log.info("Sending mail to SOFD admin");
			String subject = "Der er " + count + " nye adviser";
			String message = "Til SOFD Administratoren.<br/><p>Der er dannet " + count + " nye adviser i SOFD som skal behandles.</p><p>Log venligst ind i SOFD og behandl disse adviser</p>";
			
			emailService.sendMessage(configuration.getCustomer().getNotificationEmail(), subject, message, null, null, null,"Mail om adviser til SOFD Administratoren");
			log.info("done sending mail to SOFD admin");
		}

		var adWithBadEmployeeIdNotifications = notificationService.generateADWithBadEmployeeIdNotifications();
		log.info("adWithBadEmployeeIdNotifications count: " + adWithBadEmployeeIdNotifications);
		count += adWithBadEmployeeIdNotifications;
		
		var futureADWithBadEmployeeIdNotifications = notificationService.generateFutureADWithBadEmployeeIdNotifications();
		log.info("futureADWithBadEmployeeIdNotifications count: " + futureADWithBadEmployeeIdNotifications);
		count += futureADWithBadEmployeeIdNotifications;

		var deletedParentOrgUnitNotifications = notificationService.generateDeletedParentOrgUnitNotifications();
		log.info("deletedParentOrgUnitNotifications count: " + deletedParentOrgUnitNotifications);
		count += deletedParentOrgUnitNotifications;

        long manualNotifications = notificationService.generateManualNotifications();
        log.info("manualNotifications count: " + manualNotifications);
        count += (int) manualNotifications;
		
		log.info("Completed notification generation with " + count + " new notifications");
	}
}
