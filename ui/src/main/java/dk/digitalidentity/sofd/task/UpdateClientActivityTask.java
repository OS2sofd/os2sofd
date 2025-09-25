package dk.digitalidentity.sofd.task;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.ClientDao;
import dk.digitalidentity.sofd.dao.SecurityLogDao;
import dk.digitalidentity.sofd.dao.model.Client;
import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class UpdateClientActivityTask {

	@Autowired
	private SofdConfiguration configuration;

	// using DAO to avoid interceptors - as we don't actually want to audit this technical timestamp update
	@Autowired
	private ClientDao clientDao;

	@Autowired
	private SecurityLogDao securityLogDao;

	// run every hour
	@Scheduled(cron = "0 #{new java.util.Random().nextInt(60)} * * * ?")
	@Transactional(rollbackFor = Exception.class)
	public void updateTimestamps() {
		if (!configuration.getScheduled().isEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}

		LocalDateTime tts = LocalDateTime.now().minusMinutes(65);
		
		for (Client client : clientDao.findAll()) {
			Date date = securityLogDao.getLastTimestampByClientId(client.getId(), tts);
			if (date != null) {
				clientDao.setLastActive(date, client.getId());
			}
		}
	}
	
	// run every hour
	@Scheduled(cron = "0 0 * * * ?")
	@Transactional(rollbackFor = Exception.class)
	public void checkForActivity() {
		if (!configuration.getScheduled().isEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}

		Calendar cal = Calendar.getInstance();

		for (Client client : clientDao.findAll()) {
			if (!client.isMonitorForActivity()) {
				continue;
			}

			cal.setTime(new Date()); //Reset the Calendar per client
			cal.add(Calendar.HOUR_OF_DAY, -client.getErrorStateHours());
			Date errorStateHours = cal.getTime();

			if (client.getLastActive() != null && client.getLastActive().before(errorStateHours)) {
				log.error("Client has not seen activity for " + client.getErrorStateHours() + " hours: " + client.getName() + " / " + client.getId());
			}
		}
	}
}
