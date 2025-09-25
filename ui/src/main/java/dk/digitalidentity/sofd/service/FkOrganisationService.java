package dk.digitalidentity.sofd.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.os2sync.dto.FKHierarchy;
import dk.digitalidentity.sofd.service.os2sync.dto.FKHierarchyWrapper;
import dk.digitalidentity.sofd.service.os2sync.dto.FKOU;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FkOrganisationService {

	@Autowired
	private SofdConfiguration configuration;

	@Qualifier("defaultRestTemplate")
	@Autowired
	private RestTemplate restTemplate;

	public List<FKOU> getOrgUnits() throws Exception {
		log.info("Attempting to fetch organisation data from FK Organisation");

		HttpHeaders headers = new HttpHeaders();
		headers.add("Cvr", configuration.getCustomer().getCvr());
		headers.add("Accept", "application/json");
		headers.add("onlyOUs", "true");
		HttpEntity<String> request = new HttpEntity<>(headers);

		ResponseEntity<String> keyResponse = restTemplate.exchange(configuration.getIntegrations().getOs2sync().getRestUrl(), HttpMethod.GET, request, String.class);

		if (keyResponse.getStatusCodeValue() != 200) {
			throw new Exception("Synchronization (getKey) failed: " + keyResponse.getStatusCodeValue());
		}

		String key = keyResponse.getBody().replace("\"", "");

		ResponseEntity<FKHierarchyWrapper> response = null;
		int maxAttempts = 60;

		for (int i = 1; i <= maxAttempts; i++) {
			Thread.sleep(5 * 1000); // sleep 5 seconds before attempting to read again

			try {
				response = restTemplate.exchange(configuration.getIntegrations().getOs2sync().getRestUrl() + "/" + key, HttpMethod.GET, request, FKHierarchyWrapper.class);

				if (response.getStatusCodeValue() != 404) {
					break;
				}
			}
			catch (HttpStatusCodeException e) {
				log.warn("Failed to get hierarchy for key " + key + ". Attempt " + i + " of " + maxAttempts + ". status=" + e.getRawStatusCode());
			}
		}

		if (response == null) {
			log.error("Synchronization (getResponse) failed: Timeout");
			throw new Exception("Synchronization (getResponse) failed: Timeout");
		}

		if (response.getStatusCodeValue() != 200) {
			log.error("Synchronization (getResponse) failed: " + response.getStatusCodeValue());
			throw new Exception("Synchronization (getResponse) failed: " + response.getStatusCodeValue());
		}

		FKHierarchyWrapper hierarchyWrapper = response.getBody();

		if (hierarchyWrapper.getStatus() != 0) {
			throw new Exception("Synchronization (HierarchyStatus) failed: " + hierarchyWrapper.getStatus());
		}

		FKHierarchy hierarchy = hierarchyWrapper.getResult();
		log.info("Successfully fetched orgunit Hierarcy. OU count: " + hierarchy.getOus().size() + ".");

		return hierarchy.getOus();
	}
}
