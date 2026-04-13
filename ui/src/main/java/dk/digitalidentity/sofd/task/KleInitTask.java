package dk.digitalidentity.sofd.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.KleService;

@Component
public class KleInitTask {

    @Autowired
    private SofdConfiguration configuration;

    @Autowired
    private KleService kleService;

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        if (configuration.getScheduled().isEnabled() && kleService.countByActiveTrue() == 0) {
            kleService.updateCache();
        }
        else {
            // even non scheduled instances should populate the cache
            kleService.reloadCache(true);
        }
    }
}
