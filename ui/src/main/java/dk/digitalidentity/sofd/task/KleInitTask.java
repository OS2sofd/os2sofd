package dk.digitalidentity.sofd.task;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.KleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;

@Component
@EnableAsync
public class KleInitTask {

    @Autowired
    private SofdConfiguration configuration;

    @Autowired
    private KleService kleService;

    @EventListener(ApplicationReadyEvent.class)
    @Async
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
