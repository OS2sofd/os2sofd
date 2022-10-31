package dk.digitalidentity.sofd.task;

import java.util.Calendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.SynchronizePersonChildrenService;
import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class SynchronizePersonChildrenTask {

	@Autowired
	private SofdConfiguration configuration;

    @Autowired
    private SynchronizePersonChildrenService synchronizePersonChildrenService;

    @EventListener
    public void onApplicationEvent(ContextStartedEvent event) {
        if (configuration.getScheduled().getChildrenSync().isRunOnStartup()) {
            updateChildren();
        }
    }

    @Scheduled(cron = "0 0 22 * * ?")
    public void updateChildren() {
        if (!configuration.getScheduled().isEnabled() || !configuration.getScheduled().getChildrenSync().isEnabled()) {
            log.debug("Scheduled update of children are disabled on this instance");
            return;
        }
        
        log.info("Running scheduled update of children");

        Calendar cal = Calendar.getInstance();
        int digit = (cal.get(Calendar.DATE) % 10);

        synchronizePersonChildrenService.updateChildrenOnAllWithLastCprDigit(Integer.toString(digit));
    }
}