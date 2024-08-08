package dk.digitalidentity.sofd.log;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.dao.SecurityLogDao;
import dk.digitalidentity.sofd.dao.model.SecurityLog;
import dk.digitalidentity.sofd.security.SecurityUtil;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SecurityLogger {

	@Autowired
	private SecurityLogDao securityLogDao;

	public void log(String ipAddress, String method, String request, int status, long processedTime) {
		if (shouldLog(request)) {
			SecurityLog entry = new SecurityLog();

			if (SecurityUtil.getClient() != null) {
				entry.setClientId(SecurityUtil.getClient().getEntityId());
				entry.setClientname(SecurityUtil.getClient().getName());
			}
			else {
				log.error("Failed to identify client during security log!");
				entry.setClientId("UNKNOWN!");
				entry.setClientname("UNKNOWN!");
			}

			entry.setTimestamp(new Date());
			entry.setIpAddress(ipAddress);
			entry.setMethod(method);
			entry.setRequest(request);
			entry.setStatus(status);
			entry.setProcessedTime(processedTime);

			securityLogDao.save(entry);			
		}
	}
	
	private boolean shouldLog(String request) {
		return !(request.startsWith("uri=/api/sync/orgunits") ||
				 request.startsWith("uri=/api/sync/persons") ||
				 request.startsWith("uri=/api/sync/head"));
	}
}