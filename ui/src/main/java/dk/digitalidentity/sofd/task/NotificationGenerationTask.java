package dk.digitalidentity.sofd.task;

import java.util.Random;

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
	
	// spread tenants out over a 60-minute window starting at 04:15 to avoid all instances
	// hammering shared infra at the same second
	public static String fuzzedNotificationCron() {
		int totalMinutes = 15 + new Random().nextInt(60);
		int minute = totalMinutes % 60;
		int hour = 4 + (totalMinutes / 60);
		return "0 " + minute + " " + hour + " * * ?";
	}

	@Scheduled(cron = "${cron.notification.task:#{T(dk.digitalidentity.sofd.task.NotificationGenerationTask).fuzzedNotificationCron()}}")
	public void processChanges() {
		if (!configuration.getScheduled().isEnabled()) {
			return;
		}
		
		log.info("Starting notification generation");
		long taskStart = System.currentTimeMillis();

		int count = 0;
		if (configuration.getModules().getAccountCreation().isEnabled()) {
			long t = System.currentTimeMillis();
			var missingRulesNotifications = notificationService.generateMissingRulesNotifications();
			log.info("missingRulesNotifications count: " + missingRulesNotifications + " (" + (System.currentTimeMillis() - t) + "ms)");
			count += missingRulesNotifications;

			t = System.currentTimeMillis();
			var missingRulesNotificationsTitles = notificationService.generateMissingRulesTitlesNotifications();
			log.info("missingRulesNotificationsTitles count: " + missingRulesNotificationsTitles + " (" + (System.currentTimeMillis() - t) + "ms)");
			count += missingRulesNotificationsTitles;

			t = System.currentTimeMillis();
			var usersNotSupportedByRuleNotifications = notificationService.generateUsersNotSupportedByRuleNotifications();
			log.info("usersNotSupportedByRuleNotifications count: " + usersNotSupportedByRuleNotifications + " (" + (System.currentTimeMillis() - t) + "ms)");
			count += usersNotSupportedByRuleNotifications;
		}

		if (count > 0 && StringUtils.hasLength(configuration.getCustomer().getNotificationEmail())) {
			log.info("Sending mail to SOFD admin");
			String subject = "Der er " + count + " nye adviser";
			String message = "Til SOFD Administratoren.<br/><p>Der er dannet " + count + " nye adviser i SOFD som skal behandles.</p><p>Log venligst ind i SOFD og behandl disse adviser</p>";

			emailService.sendMessage(configuration.getCustomer().getNotificationEmail(), subject, message, null, null, null,"Mail om adviser til SOFD Administratoren");
			log.info("done sending mail to SOFD admin");
		}

		long t = System.currentTimeMillis();
		var adWithBadEmployeeIdNotifications = notificationService.generateADWithBadEmployeeIdNotifications();
		log.info("adWithBadEmployeeIdNotifications count: " + adWithBadEmployeeIdNotifications + " (" + (System.currentTimeMillis() - t) + "ms)");
		count += adWithBadEmployeeIdNotifications;

		t = System.currentTimeMillis();
		var futureADWithBadEmployeeIdNotifications = notificationService.generateFutureADWithBadEmployeeIdNotifications();
		log.info("futureADWithBadEmployeeIdNotifications count: " + futureADWithBadEmployeeIdNotifications + " (" + (System.currentTimeMillis() - t) + "ms)");
		count += futureADWithBadEmployeeIdNotifications;

		t = System.currentTimeMillis();
		var deletedParentOrgUnitNotifications = notificationService.generateDeletedParentOrgUnitNotifications();
		log.info("deletedParentOrgUnitNotifications count: " + deletedParentOrgUnitNotifications + " (" + (System.currentTimeMillis() - t) + "ms)");
		count += deletedParentOrgUnitNotifications;

		t = System.currentTimeMillis();
        long manualNotifications = notificationService.generateManualNotifications();
        log.info("manualNotifications count: " + manualNotifications + " (" + (System.currentTimeMillis() - t) + "ms)");
        count += (int) manualNotifications;

		log.info("Completed notification generation with " + count + " new notifications in " + (System.currentTimeMillis() - taskStart) + "ms");
	}
}
