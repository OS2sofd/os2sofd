package dk.digitalidentity.sofd.task;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.FutureEmail;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.service.EmailQueueService;
import dk.digitalidentity.sofd.service.EmailTemplateService;
import dk.digitalidentity.sofd.service.FutureEmailsService;
import dk.digitalidentity.sofd.service.PersonService;
import lombok.extern.slf4j.Slf4j;

import javax.transaction.Transactional;

@Slf4j
@Component
@EnableScheduling
public class FutureEmailsTask {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private FutureEmailsService futureEmailsService;

	@Autowired
	private EmailQueueService emailQueueService;

	@Autowired
	private FutureEmailsTask self;

	@Scheduled(cron = "0 0/15 * ? * *")
	public void processEmails() {
		if (!configuration.getScheduled().isEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}

		self.executeTask();
	}

	@Transactional
	public void executeTask() {
		List<FutureEmail> futureEmails = futureEmailsService.getAllToSend(new Date());

		for (FutureEmail futureEmail : futureEmails) {
			if (futureEmail.isAllOrNone() && !futureEmail.isEboks()) {
				List<Person> personsWithEmail = futureEmail.getPersons().stream().filter(p -> PersonService.getEmail(p) != null).collect(Collectors.toList());
				if (futureEmail.getPersons().size() != personsWithEmail.size()) {
					log.info("One of the recievers is missing a email adress");

					futureEmailsService.delete(futureEmail);
					continue;
				}
			}

			for (Person person : futureEmail.getPersons()) {
				if (futureEmail.isEboks()) {
					String message = futureEmail.getMessage();
					message = message.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, PersonService.getName(person));

					emailQueueService.queueEboks(person.getCpr(), futureEmail.getTitle(), message, 0, null);
				}
				else {
					String email = PersonService.getEmail(person);
					if (email != null) {
						String message = futureEmail.getMessage();
						message = message.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, PersonService.getName(person));

						emailQueueService.queueEmail(email, futureEmail.getTitle(), message, 0, null);
					}
				}
			}

			futureEmailsService.delete(futureEmail);
		}
	}
}
