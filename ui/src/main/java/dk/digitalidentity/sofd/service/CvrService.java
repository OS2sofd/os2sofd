package dk.digitalidentity.sofd.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.dto.PUnitDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.PnrSearchResultDTO;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Post;
import dk.digitalidentity.sofd.dao.model.mapping.OrgUnitPostMapping;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.model.CvrLookupDTO;
import dk.digitalidentity.sofd.service.model.PUnitLookupDTO;
import dk.digitalidentity.sofd.service.model.PnrLookupDTO;
import dk.digitalidentity.sofd.service.model.PostLookupDTO;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@EnableCaching
public class CvrService {

	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired 
	private CvrService self;
	
	@Autowired
	private OrgUnitService orgUnitService;

	@SneakyThrows
	public void cvrMaintenance() {
		SecurityUtil.fakeLoginSession();
		Map<String, PUnitDTO> cvrMap = new HashMap<String, PUnitDTO>();

		for (OrgUnit orgUnit : orgUnitService.getAll(o -> {
			o.getPostAddresses().stream().forEach(pa -> pa.getPost().getMasterId());
		})) {
			boolean changes = false;
			List<Post> postsWithMasterCvr = OrgUnitService.getPosts(orgUnit).stream().filter(p -> p.getMaster().equals("CVR")).collect(Collectors.toList());

			// add a cvr post if one doesn't exist
			if (orgUnit.getPnr() != null && postsWithMasterCvr.size() == 0) {
				PUnitDTO pUnitDTO = cvrMap.get(orgUnit.getPnr().toString());
				if (pUnitDTO == null) {
					pUnitDTO = getPUnitByPnr(orgUnit.getPnr().toString());

					if (pUnitDTO != null) {
						cvrMap.put(orgUnit.getPnr().toString(), pUnitDTO);
					}
				}

				if (pUnitDTO != null) {
					Post post = new Post();
					post.setMaster("CVR");
					post.setMasterId(orgUnit.getPnr().toString());
					post.setCountry("Danmark");
					String street = pUnitDTO.getStreet() + " " + pUnitDTO.getNumber();
					post.setStreet(street);
					post.setPostalCode(pUnitDTO.getPostalCode());
					post.setCity(pUnitDTO.getCity());
					post.setPrime(orgUnit.getPostAddresses().isEmpty());
					OrgUnitPostMapping postMapping = new OrgUnitPostMapping();
					postMapping.setOrgUnit(orgUnit);
					postMapping.setPost(post);
					orgUnit.getPostAddresses().add(postMapping);
					changes = true;
				}
			}

			// update existing cvr posts
			for (Post post : postsWithMasterCvr) {
				PUnitDTO pUnitDTO = cvrMap.get(post.getMasterId());

				if (pUnitDTO == null) {
					pUnitDTO = getPUnitByPnr(post.getMasterId());
					cvrMap.put(post.getMasterId(),pUnitDTO);
				}

				if (pUnitDTO != null) {
					String street = pUnitDTO.getStreet() + " " + pUnitDTO.getNumber();
					if (!Objects.equals(post.getStreet(), street)) {
						post.setStreet(street);
						changes = true;
					}

					if (!Objects.equals(post.getPostalCode(), pUnitDTO.getPostalCode())) {
						post.setPostalCode(pUnitDTO.getPostalCode());
						changes = true;
					}

					if (!Objects.equals(post.getCity(), pUnitDTO.getCity())) {
						post.setCity(pUnitDTO.getCity());
						changes = true;
					}
				}
			}

			if (orgUnit.getPnr() != null) {
				PUnitDTO pUnitDTO = cvrMap.get(orgUnit.getPnr().toString());
				if (pUnitDTO == null) {
					pUnitDTO = getPUnitByPnr(orgUnit.getPnr().toString());
					if (pUnitDTO != null) {
						cvrMap.put(orgUnit.getPnr().toString(), pUnitDTO);
					}
				}

				if (pUnitDTO != null) {
					// update cvr name
					// it is on purpose we only update if we get a non-null response from cvrService (to prevent nulling cvrName field if service is down or fails).
					if (!Objects.equals(orgUnit.getCvrName(), pUnitDTO.getName())) {
						orgUnit.setCvrName(pUnitDTO.getName());
						changes = true;
					}
				}
			}

			if (changes) {
				orgUnitService.save(orgUnit);
			}
		}
	}
	
	public PUnitDTO getPUnitByPnr(String pnr) {
		
		if (!configuration.getIntegrations().getCvr().isEnabled()) {
			return null;
		}
		
		// no reason to lookup invalid p numbers
		if (!validPnr(pnr)) {
			return null;
		}
		
		RestTemplate restTemplate = new RestTemplate();

		String cvrResourceUrl = configuration.getIntegrations().getCvr().getBaseUrl();
		if (!cvrResourceUrl.endsWith("/")) {
			cvrResourceUrl += "/";
		}
		cvrResourceUrl += "CVR/HentCVRData/1/rest/hentProduktionsenhedMedPNummer?ppNummer=" + pnr;
		
		String apiKey = configuration.getIntegrations().getCvr().getApiKey();
		HttpEntity<String> request = new HttpEntity<>(getHeaders(apiKey));

		try {
			ResponseEntity<PUnitLookupDTO> response = restTemplate.exchange(cvrResourceUrl, HttpMethod.GET, request, PUnitLookupDTO.class);

			PostLookupDTO post = response.getBody().getPost();
			PUnitDTO dto = new PUnitDTO(post.getNumber(), post.getStreet(), post.getPostalCode(), post.getCity(), response.getBody().getPUnitName().getValue());
			if(!dto.isValid())
			{
				log.warn("Not using cvr response because it is not valid: " + pnr);
				return null;
			}
			return dto;
		}
		catch (RestClientException ex) {
			log.warn("Failed to lookup: " + pnr, ex);

			return null;
		}
	}
	
	public List<PnrSearchResultDTO> getSearchResultByPnr(String pnr) {
		if (!configuration.getIntegrations().getCvr().isEnabled()) {
			return null;
		}
		
		// no reason to lookup invalid p numbers
		if (!validPnr(pnr)) {
			return null;
		}
		
		RestTemplate restTemplate = new RestTemplate();

		String cvrResourceUrl = configuration.getIntegrations().getCvr().getBaseUrl();
		if (!cvrResourceUrl.endsWith("/")) {
			cvrResourceUrl += "/";
		}
		cvrResourceUrl += "CVR/HentCVRData/1/rest/hentProduktionsenhedMedPNummer?ppNummer=" + pnr;
		
		String apiKey = configuration.getIntegrations().getCvr().getApiKey();
		HttpEntity<String> request = new HttpEntity<>(getHeaders(apiKey));

		try {
			ResponseEntity<PUnitLookupDTO> response = restTemplate.exchange(cvrResourceUrl, HttpMethod.GET, request, PUnitLookupDTO.class);
			List<PnrSearchResultDTO> result = new ArrayList<>();
			result.add(PnrSearchResultDTO.builder().pnr(pnr).pnrName(response.getBody().getPUnitName().getValue()).build());
			return result;
		}
		catch (RestClientException ex) {
			log.warn("Failed to lookup: " + pnr, ex);

			return null;
		}
	}
	
	public List<PnrSearchResultDTO> getSearchResultByOwnCvrAndPnr(String pnr) {
		
		if (!configuration.getIntegrations().getCvr().isEnabled()) {
			return null;
		}
		
		// no reason to lookup invalid p numbers
		if (!validPnr(pnr)) {
			return null;
		}
		String cvr = configuration.getCustomer().getCvr();
		List<PnrSearchResultDTO> fromCvr = self.getSearchResultByCvr(cvr);
			
		List<PnrSearchResultDTO> result = fromCvr.stream().filter(p -> p.getPnr().equals(pnr)).collect(Collectors.toList());

		return result;
	}
	
	public List<PnrSearchResultDTO> getSearchResultAllPnrsByName(String name) {
		
		if (!configuration.getIntegrations().getCvr().isEnabled()) {
			return null;
		}
		
		RestTemplate restTemplate = new RestTemplate();

		String cvrResourceUrl = configuration.getIntegrations().getCvr().getBaseUrl();
		if (!cvrResourceUrl.endsWith("/")) {
			cvrResourceUrl += "/";
		}
		cvrResourceUrl += "CVR/SoegCVRData/1/rest/soegProduktionsenhedMedNavn?pNavn=" + name;
		
		String apiKey = configuration.getIntegrations().getCvr().getApiKey();
		HttpEntity<String> request = new HttpEntity<>(getHeaders(apiKey));

		try {
			ResponseEntity<List<PnrLookupDTO>> response = restTemplate.exchange(cvrResourceUrl, HttpMethod.GET, request, new ParameterizedTypeReference<List<PnrLookupDTO>>() {});
			
			List<PnrSearchResultDTO> result = response.getBody().stream().map(p -> PnrSearchResultDTO.builder()
					.pnr(p.getPnr())
					.pnrName(p.getPUnitName().getValue())
					.build()).collect(Collectors.toList());
			
			return result;
		}
		catch (RestClientException ex) {
			log.warn("Failed to lookup: " + name, ex);

			return null;
		}
	}
	
	public List<PnrSearchResultDTO> getSearchResultPnrsForOwnCvrByName(String name) {
		
		if (!configuration.getIntegrations().getCvr().isEnabled()) {
			return null;
		}
		
		String cvr = configuration.getCustomer().getCvr();

		List<PnrSearchResultDTO> cvrSearchResult = self.getSearchResultByCvr(cvr);
		if( cvrSearchResult == null )
		{
			log.warn("No p-units found for cvr: " + cvr);
			return null;
		}
		List<PnrSearchResultDTO> result = cvrSearchResult.stream().filter(p -> p.getPnrName().toLowerCase().contains(name.toLowerCase())).collect(Collectors.toList());
		return result;
	}

	public boolean validPnr(String pnr) {
		if (pnr == null || pnr.length() != 10) {
			return false;
		}
		
		for (char c : pnr.toCharArray()) {
			if (!Character.isDigit(c)) {
				return false;
			}
		}
		return true;
	}
	
	private boolean validCvr(String cvr) {
		if (cvr == null || cvr.length() != 8) {
			return false;
		}
		
		for (char c : cvr.toCharArray()) {
			if (!Character.isDigit(c)) {
				return false;
			}
		}
		return true;
	}
	
	private HttpHeaders getHeaders(String apiKey) {
		HttpHeaders headers = new HttpHeaders();
		headers.add("apiKey", apiKey);
		headers.add("Content-Type", "application/json");

		return headers;
	}
	
	@Cacheable(value = "getSearchResultByCvr")
	public List<PnrSearchResultDTO> getSearchResultByCvr(String cvr) {
		if (!configuration.getIntegrations().getCvr().isEnabled()) {
			return null;
		}
		
		// no reason to lookup invalid cvr numbers
		if (!validCvr(cvr)) {
			return null;
		}
		
		
		RestTemplate restTemplate = new RestTemplate();

		String cvrResourceUrl = configuration.getIntegrations().getCvr().getBaseUrl();
		if (!cvrResourceUrl.endsWith("/")) {
			cvrResourceUrl += "/";
		}
		cvrResourceUrl += "CVR/HentCVRData/1/rest/hentVirksomhedMedCVRNummer?pCVRNummer=" + cvr + "&pInkluderProduktionsenhedsnumre=true";
		
		String apiKey = configuration.getIntegrations().getCvr().getApiKey();
		HttpEntity<String> request = new HttpEntity<>(getHeaders(apiKey));
		
		try {
			ResponseEntity<CvrLookupDTO> response = restTemplate.exchange(cvrResourceUrl, HttpMethod.GET, request, CvrLookupDTO.class);
			
			List<PnrSearchResultDTO> result = response.getBody().getPnrs().stream().map(p -> PnrSearchResultDTO.builder()
					.pnr(p.getPnr())
					.pnrName(p.getPUnitName().getValue())
					.build()).collect(Collectors.toList());
			
			return result;
		}
		catch (RestClientException ex) {
			log.warn("Failed to lookup: " + cvr, ex);

			return null;
		}
	}
	
	@Scheduled(fixedRate = 60 * 60 * 1000)
    public void resetCvrSearchResultCacheTask() {
    	self.resetCvrSearchResultCache();
    }

    @CacheEvict(value = "getSearchResultByCvr", allEntries = true)
    public void resetCvrSearchResultCache() {
    	; // clears cache every hour
    }
}
