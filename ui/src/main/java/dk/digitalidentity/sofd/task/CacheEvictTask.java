package dk.digitalidentity.sofd.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.service.EmailTemplateService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@EnableScheduling
public class CacheEvictTask {

	@Autowired
	private EmailTemplateService emailTemplateService;

    @Scheduled(fixedDelay = 15 * 60 * 1000)
    public void every15Minutes() {
    	emailTemplateService.resetEmailTemplateCache();
    }
}