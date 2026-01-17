package dk.digitalidentity.sofd.service;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.dto.BadStateDTO;
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

		if (configuration.getIntegrations().getCpr().isUseAddressName()) {
			cprResourceUrl += "&useAddressName=true";
		}

		try {
			ResponseEntity<CprLookupDTO> response = restTemplate.getForEntity(cprResourceUrl, CprLookupDTO.class);

			var result = response.getBody();
			
			// remove all address information if addresses are not enabled
			if (!configuration.getIntegrations().getCpr().isAddressEnabled()) {
				result.removeAddressInfo();
			}

			return result;
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
	
	public List<BadStateDTO> getBadStates() {
		RestTemplate restTemplate = new RestTemplate();

		String cprResourceUrl = configuration.getIntegrations().getCpr().getUrl();
		if (!cprResourceUrl.endsWith("/")) {
			cprResourceUrl += "/";
		}
		cprResourceUrl += "api/badState";
		
		log.info("Fetching badStates from '" + cprResourceUrl + "'");

		try {
			HttpHeaders headers = new HttpHeaders();
			headers.add("Accept", "application/json");

			HttpEntity<String> request = new HttpEntity<>(headers);
			ResponseEntity<BadStateDTO[]> response = restTemplate.exchange(cprResourceUrl, HttpMethod.GET, request, BadStateDTO[].class);

			return Arrays.asList(response.getBody());
		}
		catch (Exception ex) {
			log.error("Failed to get bad states", ex);

			return null;
		}
	}

	private boolean validCpr(String cpr) {
		return (!PersonService.isFictionalCpr(cpr));
	}
}
