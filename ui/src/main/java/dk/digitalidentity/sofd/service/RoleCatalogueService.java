package dk.digitalidentity.sofd.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.service.model.SystemRoleDTO;
import dk.digitalidentity.sofd.service.rc.dto.ItSystemDTO;
import dk.digitalidentity.sofd.service.rc.dto.UserReadWrapperDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@EnableCaching
@EnableScheduling
public class RoleCatalogueService {

	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired
	private RoleCatalogueService self;

	public List<ItSystemDTO> fetchAllItSystems() {
		if (!StringUtils.hasLength(configuration.getIntegrations().getRoleCatalogue().getUrl())) {
			log.warn("RC integration is not enabled!");
			return new ArrayList<>();
		}
		
		RestTemplate restTemplate = new RestTemplate();

		String resourceUrl = configuration.getIntegrations().getRoleCatalogue().getUrl();
		if (!resourceUrl.endsWith("/")) {
			resourceUrl += "/";
		}
		resourceUrl += "itsystem/all";

		HttpHeaders headers = new HttpHeaders();
		headers.add("ApiKey", configuration.getIntegrations().getRoleCatalogue().getApiKey());

		HttpEntity<List<ItSystemDTO>> request = new HttpEntity<>(headers);

		try {
			ResponseEntity<List<ItSystemDTO>> response = restTemplate.exchange(resourceUrl, HttpMethod.GET, request, new ParameterizedTypeReference<List<ItSystemDTO>>() { });

			return response.getBody();
		}
		catch (RestClientException ex) {
			log.error("Error occured while connecting to Role Catalogue", ex);

			return null;
		}
	}
	
	// every 2 hours, we wipe the cache
	@Scheduled(fixedDelay = 2 * 60 * 60 * 1000)
	public void cleanupCache() {
		self.wipeCache();
	}
	
	@CacheEvict(value = "systemRolesForItSystem", allEntries = true)
	public void wipeCache() {
		;
	}
	
	@Cacheable("systemRolesForItSystem")
	public List<SystemRoleDTO> getSystemRolesByItSystem(String identifier) {
		if (!StringUtils.hasLength(configuration.getIntegrations().getRoleCatalogue().getUrl())) {
			log.warn("RC integration is not enabled!");
			return new ArrayList<>();
		}

		RestTemplate restTemplate = new RestTemplate();

		String resourceUrl = configuration.getIntegrations().getRoleCatalogue().getUrl();
		if (!resourceUrl.endsWith("/")) {
			resourceUrl += "/";
		}
		resourceUrl += "v2/itsystem/" + identifier + "/systemroles";

		HttpHeaders headers = new HttpHeaders();
		headers.add("ApiKey", configuration.getIntegrations().getRoleCatalogue().getApiKey());

		HttpEntity<List<SystemRoleDTO>> request = new HttpEntity<>(headers);

		try {
			ResponseEntity<List<SystemRoleDTO>> response = restTemplate.exchange(resourceUrl, HttpMethod.GET, request, new ParameterizedTypeReference<List<SystemRoleDTO>>() { });

			List<SystemRoleDTO> result = response.getBody();
			
			Collections.sort(result, new Comparator<SystemRoleDTO>() {
				public int compare(SystemRoleDTO o1, SystemRoleDTO o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});

			return result;
		}
		catch (RestClientException ex) {
			log.error("Error occured while connecting to Role Catalogue", ex);

			return null;
		}
	}

	public Set<String> getUsersByItSystem(String identifier) {
		if (!StringUtils.hasLength(configuration.getIntegrations().getRoleCatalogue().getUrl())) {
			log.warn("RC integration is not enabled!");
			return new HashSet<>();
		}

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
