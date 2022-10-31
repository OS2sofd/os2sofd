package dk.digitalidentity.sofd.task;

import java.util.Calendar;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.SynchronizePersonAddressService;
import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class SynchronizePersonAddressesTask {

	@Autowired
	private SofdConfiguration configuration;

    @Autowired
    private SynchronizePersonAddressService synchronizePersonAddressService;

    @EventListener
    public void onApplicationEvent(ContextStartedEvent event) {
        if (configuration.getScheduled().getAddressSync().isRunOnStartup()) {
            updateAddresses();
        }
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void updateAddresses() {
        if (!configuration.getScheduled().isEnabled() || !configuration.getScheduled().getAddressSync().isEnabled()) {
            log.debug("Scheduled update of addresses are disabled on this instance");
            return;
        }

        log.info("Running scheduled update of addresses");

        Calendar cal = Calendar.getInstance();
        int digit = (cal.get(Calendar.DATE) % 10);
        
        synchronizePersonAddressService.updateAddressesOnAllWithLastCprDigit(Integer.toString(digit));
    }
}