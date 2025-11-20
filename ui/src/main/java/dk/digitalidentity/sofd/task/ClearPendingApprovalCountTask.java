package dk.digitalidentity.sofd.task;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.OS2IlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ClearPendingApprovalCountTask {

    @Autowired
    private OS2IlmService os2IlmService;

    @Autowired
    private SofdConfiguration configuration;

    @Scheduled(cron = "${cron.clearPendingApprovalCount.task: 0 */5 * * * *}")
    public void clearPendingApprovalCount() {
        if (configuration.getIntegrations().getOs2ilm().isEnabled()) {
            log.debug("Clearing pending approval count");
            os2IlmService.clearPendingApprovalCountCache();
        } else {
            log.debug("OS2ilm integration disabled, skipping clearing pending approval count cache");
        }
    }
}
