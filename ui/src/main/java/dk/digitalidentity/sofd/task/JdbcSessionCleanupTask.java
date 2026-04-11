package dk.digitalidentity.sofd.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;

@Component
@EnableScheduling
public class JdbcSessionCleanupTask {

    @Autowired
    private JdbcIndexedSessionRepository sessionRepository;
    
    @Autowired
    private SofdConfiguration sofdConfiguration;
    
    @Reschedule(cron = "#{new java.util.Random().nextInt(60)} #{new java.util.Random().nextInt(10)}/10 * * * ?")
    @Scheduled(cron = "0 0/10 * * * ?")
    public void cleanupJdbcSessions() {
    	if (sofdConfiguration.getScheduled().isEnabled()) {
    		sessionRepository.cleanUpExpiredSessions();
    	}
    }
}
