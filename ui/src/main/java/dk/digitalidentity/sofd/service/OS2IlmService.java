package dk.digitalidentity.sofd.service;


import dk.digitalidentity.sofd.config.SofdConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Service
public class OS2IlmService {

    @Qualifier("trustEverythingRestTemplate")
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SofdConfiguration configuration;

    @Cacheable(value = "pendingApprovalCount", unless = "#result == null")
    public Integer getPendingApprovalCount() {
        try {
            if (!StringUtils.hasText(configuration.getIntegrations().getOs2ilm().getUrl()) || !StringUtils.hasText(configuration.getIntegrations().getOs2ilm().getApiKey())) {
                log.warn("OS2ilm URL or ApiKey have not been configured and are missing");
                return null;
            }

            String restUrl = configuration.getIntegrations().getOs2ilm().getUrl() + "/api/employee/pending-approval/count";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("ApiKey", configuration.getIntegrations().getOs2ilm().getApiKey());
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Integer> response = restTemplate.exchange(
                    restUrl,
                    HttpMethod.GET,
                    entity,
                    Integer.class
            );

            // Validate response
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Received non-success status: {}", response.getStatusCode());
                return null;
            }

            Integer count = response.getBody();
            if (count == null) {
                log.warn("Received null response body");
                return null;
            }

            if (count < 0) {
                log.warn("Received negative count: {}", count);
                return null;
            }

            log.debug("Successfully retrieved pending approval count: {}", count);
            return count;

        } catch (Exception ex) {
            log.error("Error happened when calling OS2Ilm integration", ex);
            return null;
        }
    }

    @CacheEvict(value = "pendingApprovalCount", allEntries = true)
    public void clearPendingApprovalCountCache() {
        log.debug("Clearing pending approval count cache");
    }
}
