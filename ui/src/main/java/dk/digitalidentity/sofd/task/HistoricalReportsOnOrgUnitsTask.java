package dk.digitalidentity.sofd.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.xls.HistoricalReportsOnOrgUnitsService;
import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class HistoricalReportsOnOrgUnitsTask {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private HistoricalReportsOnOrgUnitsService historicalReportsOnOrgUnitsService;

	@Scheduled(cron = "${cron.historicalreportsonorgunits.task:0 0 0 1 * ?}")
	public void generateOrgunitReport() {
		if (!configuration.getScheduled().isEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}

		if (!configuration.getModules().getHistorialReportsOnOrgunits().isEnabled()) {
			log.debug("History on orgUnits is disabled");
			return;
		}
		log.info("Generating historical orgunit report");
		historicalReportsOnOrgUnitsService.generateOrgunitReport();
	}
}
