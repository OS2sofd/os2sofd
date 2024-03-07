package dk.digitalidentity.sofd.task;

import dk.digitalidentity.sofd.service.AffiliationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class DeleteInvalidAffiliationsTask {

    @Autowired
    private AffiliationService affiliationService;

    // TODO: can be deleted once it has been run on all instances.
    @EventListener(ApplicationReadyEvent.class)
    public void deleteInvalidAffiliations() {
        affiliationService.deleteInvalidOpusAffiliations();
    }
}
