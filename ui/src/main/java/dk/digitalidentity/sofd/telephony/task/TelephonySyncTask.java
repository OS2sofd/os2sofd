package dk.digitalidentity.sofd.telephony.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.telephony.controller.service.TelephonyPhoneService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@EnableScheduling
@Component
public class TelephonySyncTask {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private TelephonyPhoneService telephonyPhoneService;
	
	// run once every minute from 05:00 to 23:00
	@Scheduled(cron = "0 0/1 5-23 * * *")
	public void deltaSync() throws Exception {
		if (!configuration.getScheduled().isEnabled() || !configuration.getModules().getTelephony().isEnabled()) {
			return;
		}

		long count = telephonyPhoneService.deltaSync();
		if (count > 0) {
			log.info("DeltaSync: updated " + count + " phone(s)");
		}
	}

	// run 03:10 every night (TODO: randomize once we have Spring Boot 2.x)
	@Scheduled(cron = "0 10 3 * * *")
	//@Scheduled(fixedDelay = 60 * 60 * 1000)
	public void fullSync() throws Exception {
		if (!configuration.getScheduled().isEnabled() || !configuration.getModules().getTelephony().isEnabled()) {
			return;
		}
		
		log.info("Starting full sync");

		long count = telephonyPhoneService.fullSync();

		log.info("Updated " + count + " phone(s)");
	}
}
