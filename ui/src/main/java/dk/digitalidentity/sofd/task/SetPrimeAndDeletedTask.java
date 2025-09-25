package dk.digitalidentity.sofd.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.PersonService;
import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class SetPrimeAndDeletedTask {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private PersonService personService;

	@Scheduled(cron = "${cron.primeAndDeleted:0 #{new java.util.Random().nextInt(10)} 6 * * ?}")
	@Transactional(rollbackFor = Exception.class)
	public void setPrimeAndDeletedTask() {
		if (!configuration.getScheduled().isEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}
		
		personService.setPrimeAffiliationPrimeUserAndDeleted();
	}
}