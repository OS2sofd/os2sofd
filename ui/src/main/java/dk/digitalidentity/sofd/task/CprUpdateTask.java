package dk.digitalidentity.sofd.task;

import java.util.Calendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.CprUpdateService;
import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class CprUpdateTask {

	@Autowired
	private SofdConfiguration configuration;

    @Autowired
    private CprUpdateService cprUpdateService;

    @EventListener
    public void onApplicationEvent(ContextStartedEvent event) {
        if (configuration.getScheduled().getCprSync().isRunOnStartup()) {
        	updateFromCpr();
        }
    }

    @Scheduled(cron = "0 #{new java.util.Random().nextInt(55)} 21 * * ?")
    public void updateFromCpr() {
        if (!configuration.getScheduled().isEnabled() || !configuration.getScheduled().getCprSync().isEnabled()) {
            log.debug("Scheduled update of addresses are disabled on this instance");
            return;
        }

        log.info("Running scheduled update of persons from cpr");

        Calendar cal = Calendar.getInstance();
        int digit = (cal.get(Calendar.DATE) % 10);
        
        cprUpdateService.updatePersonsWithLastCprDigit(Integer.toString(digit));
        
        log.info("Scheduled update of persons from cpr completed");
    }
    
    @Scheduled(cron = "${cron.cpr.badstate:0 #{new java.util.Random().nextInt(55)} 3 * * ?}")
    public void updateBadStates() {
        if (!configuration.getScheduled().isEnabled() || !configuration.getScheduled().getCprSync().isEnabled()) {
            log.debug("Scheduled update of addresses are disabled on this instance");
            return;
        }

        log.info("Running scheduled update of bad state on persons from cpr");

        cprUpdateService.updateBadState();
        
        log.info("Scheduled update of bad state on persons from cpr completed");
    }
}