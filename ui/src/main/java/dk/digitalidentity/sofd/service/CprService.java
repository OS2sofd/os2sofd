package dk.digitalidentity.sofd.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.dto.CprLookupDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CprService {

	@Autowired
	private SofdConfiguration configuration;

	public CprLookupDTO getByCpr(String cpr) {
		return getByCpr(cpr, false);
	}
	
	public CprLookupDTO getByCpr(String cpr, boolean avoidCache) {
		RestTemplate restTemplate = new RestTemplate();
		
		// no reason to lookup invalid cpr numbers
		if (!validCpr(cpr)) {
			return null;
		}

		String cprResourceUrl = configuration.getIntegrations().getCpr().getUrl();
		if (!cprResourceUrl.endsWith("/")) {
			cprResourceUrl += "/";
		}
		cprResourceUrl += "api/person?cpr=" + cpr + "&cvr=" + configuration.getCustomer().getCvr();
		if (avoidCache) {
			cprResourceUrl += "&avoidCache=true";
		}

		try {
			ResponseEntity<CprLookupDTO> response = restTemplate.getForEntity(cprResourceUrl, CprLookupDTO.class);

			return response.getBody();
		}
		catch (RestClientResponseException ex) {
			String responseBody = ex.getResponseBodyAsString();

			if (ex.getRawStatusCode() == 404 && responseBody != null && responseBody.contains("PNR not found")) {
				log.warn("Person cpr does not exists in cpr-register: " + PersonService.maskCpr(cpr));
			}
			else {
				log.warn("Failed to lookup: " + PersonService.maskCpr(cpr), ex);
			}

			return null;
		}
	}

	private boolean validCpr(String cpr) {
		return (PersonService.isFictionalCpr(cpr) == false);
	}
}
