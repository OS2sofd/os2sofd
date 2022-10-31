package dk.digitalidentity.sofd.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.rc.dto.ItSystemDTO;
import dk.digitalidentity.sofd.service.rc.dto.UserReadWrapperDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RoleCatalogueService {

	@Autowired
	private SofdConfiguration configuration;

	public List<ItSystemDTO> fetchAllItSystems() {
		RestTemplate restTemplate = new RestTemplate();

		String cprResourceUrl = configuration.getIntegrations().getRoleCatalogue().getUrl();
		if (!cprResourceUrl.endsWith("/")) {
			cprResourceUrl += "/";
		}
		cprResourceUrl += "itsystem/all";

		HttpHeaders headers = new HttpHeaders();
		headers.add("ApiKey", configuration.getIntegrations().getRoleCatalogue().getApiKey());

		HttpEntity<List<ItSystemDTO>> request = new HttpEntity<>(headers);

		try {
			ResponseEntity<List<ItSystemDTO>> response = restTemplate.exchange(cprResourceUrl, HttpMethod.GET, request, new ParameterizedTypeReference<List<ItSystemDTO>>() { });

			return response.getBody();
		}
		catch (RestClientException ex) {
			log.error("Error occured while connecting to Role Catalogue", ex);

			return null;
		}
	}

	public Set<String> getUsersByItSystem(String identifier) {
		RestTemplate restTemplate = new RestTemplate();

		String cprResourceUrl = configuration.getIntegrations().getRoleCatalogue().getUrl();
		if (!cprResourceUrl.endsWith("/")) {
			cprResourceUrl += "/";
		}
		cprResourceUrl += "read/itsystem/" + identifier + "?indirectRoles=true";

		HttpHeaders headers = new HttpHeaders();
		headers.add("ApiKey", configuration.getIntegrations().getRoleCatalogue().getApiKey());

		HttpEntity<List<UserReadWrapperDTO>> request = new HttpEntity<>(headers);

		try {
			ResponseEntity<List<UserReadWrapperDTO>> response = restTemplate.exchange(cprResourceUrl, HttpMethod.GET, request, new ParameterizedTypeReference<List<UserReadWrapperDTO>>() {});

			List<UserReadWrapperDTO> rolesWrapper = response.getBody();
			Set<String> userIds = rolesWrapper.stream()
				.filter(x -> x.getAssignments() != null)
				.flatMap(x -> x.getAssignments().stream())
				.map(x -> x.getUserId())
				.collect(Collectors.toSet());

			return userIds;
		}
		catch (RestClientException ex) {
			log.error("Error occured while connecting to Role Catalogue", ex);
			
			return new HashSet<>();
		}
	}
}
