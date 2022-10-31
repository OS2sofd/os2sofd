package dk.digitalidentity.sofd.service;

import java.util.ArrayList;
import java.util.List;
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
import dk.digitalidentity.sofd.service.model.CvrLookupDTO;
import dk.digitalidentity.sofd.service.model.PUnitLookupDTO;
import dk.digitalidentity.sofd.service.model.PnrLookupDTO;
import dk.digitalidentity.sofd.service.model.PostLookupDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@EnableCaching
public class CvrService {

	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired 
	private CvrService self;

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
