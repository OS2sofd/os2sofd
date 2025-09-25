package dk.digitalidentity.sofd.log;

import java.util.Calendar;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.AuditLogDao;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
@Transactional(rollbackFor = Exception.class)
public class AuditLogCleaner {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private AuditLogDao auditLogEntryDao;

	// run every night at 04:?? on Saturdays
	@Scheduled(cron = "#{new java.util.Random().nextInt(60)} #{new java.util.Random().nextInt(60)} 4 * * SAT")
	public void cleanupAuditLogs() {
		if (!configuration.getScheduled().isEnabled()) {
			log.info("Scheduled jobs are disabled on this instance");
			return;
		}
		log.info("Running scheduled job");

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MONTH, -6);
		Date before = cal.getTime();

		auditLogEntryDao.deleteByTimestampBefore(before);
	}
}