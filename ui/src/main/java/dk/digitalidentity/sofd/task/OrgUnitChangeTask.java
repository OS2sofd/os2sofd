package dk.digitalidentity.sofd.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.service.OrgUnitChangeService;

@Component
@EnableScheduling
public class OrgUnitChangeTask {

    @Autowired
    private OrgUnitChangeService orgUnitChangeService;

    @Scheduled(cron = "0 0 18 ? * *")
    public void processOrgUnitChanges() {
        orgUnitChangeService.processOrgUnitChanges();
    }
}