package dk.digitalidentity.sofd.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import dk.digitalidentity.sofd.dao.model.AuthorizationCode;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.mapping.PersonAuthorizationCodeMapping;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.model.ArrayOfHealthProfessional;
import dk.digitalidentity.sofd.service.model.HealthProfessional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AuthorizationCodeService {

    @Autowired
    private PersonService personService;

    @Transactional
	public void syncAll(boolean forceAll) {
    	SecurityUtil.fakeLoginSession();

    	// we sync every 10 days (or 10% of users every day so to speak ;))
		long dayOfMonthDigit = LocalDate.now().getDayOfMonth() % 10;

		for (Person person : personService.getActive()) {
	    	if (!person.isHasUpdatedAuthorizationCode() || forceAll || Long.parseLong(person.getCpr().substring(9, 10)) == dayOfMonthDigit) {
	    		syncAuthorizationCodes(person);
	    	}
		}
	}
    
    public void syncAuthorizationCodes(Person person) {
    	
    	// we only do a forced update once (and yes, if the call fails, then we only retry once every 10 days
		if (!person.isHasUpdatedAuthorizationCode()) {
			person.setHasUpdatedAuthorizationCode(true);
			personService.save(person);
		}
		
    	String name = person.getFirstname() + " " + person.getSurname();
    	String day = person.getCpr().substring(0, 2);
    	String month = person.getCpr().substring(2, 4);
    	String yearString = person.getCpr().substring(4, 6);
    	int year = Integer.parseInt(yearString);
    	
    	switch (person.getCpr().charAt(6)) {
	    	case '0':
	    	case '1':
	    	case '2':
	    	case '3':
	    		yearString = "19" + yearString;
	    		break;
	    	case '4':
	    	case '9':
	    		if (year <= 36) {
		    		yearString = "20" + yearString;		    			
	    		}
	    		else {
		    		yearString = "19" + yearString;
	    		}
	    		break;
	    	case '5':
	    	case '6':
	    	case '7':
	    	case '8':
	    		if (year <= 57) {
		    		yearString = "20" + yearString;		    			
	    		}
	    		else {
		    		yearString = "18" + yearString;
	    		}
	    		break;
			default:
				return;
		}
    	
    	String date = yearString + "-" + month + "-" + day;
    	
    	// verify it is an actual CPR (robots and stuff will be skipped here)
    	try {
    		LocalDate.parse(date);
    	}
    	catch (Exception ex) {
    		return;
    	}
    	
    	RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHeaders();
        String url = "https://autregwebservice.sst.dk/autregservice.asmx/GetHealthProfessionals?name=" + name + "&authorizationId=\"\"&birthdayFrom=" + date + "&birthdayTo=" + date + "&authorizationDateFrom=1950-01-01&authorizationDateTo=2030-01-01&professionGroup=\"\"&specialityName=\"\"&authorizationStatus=NotSpecified&seventyFiveYearsRule=NotSpecified";

        HttpEntity<String> request = new HttpEntity<>(headers);
        
        ArrayOfHealthProfessional authResponse = null;

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
            	String body = response.getBody();
            	if (body == null) {
            		log.warn("Failed to fetch authorization code for person with uuid " + person.getUuid() + ". Body is null");
            		return;
            	}
            	
            	XmlMapper xmlMapper = XmlMapper.builder()
												.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
												.defaultUseWrapper(false)
												.addModule(new JavaTimeModule())
												.build();
    			try {
					authResponse = xmlMapper.readValue(body, ArrayOfHealthProfessional.class);
				}
    			catch (Exception ex) {
					log.error("Failed to fetch authorization code for person with uuid " + person.getUuid() + ". Could not parse xml", ex);
					return;
				}
            }
        }
        catch (HttpClientErrorException ex) {
            log.warn("Failed to fetch authorization code for person with uuid " + person.getUuid() + ". Error: " + ex.getResponseBodyAsString(), ex);
            return;
        }

        boolean changes = false;

		if (authResponse.getHealthProfessionals() == null || authResponse.getHealthProfessionals().isEmpty()) {

			// did the person loose his or her authorization codes?
			if (person.getAuthorizationCodes() != null && person.getAuthorizationCodes().size() > 0) {
				person.getAuthorizationCodes().clear();
				changes = true;
			}
        }
        else {
        	List<AuthorizationCode> sortedAndValidAuthorizationCodes = authResponse.getHealthProfessionals().stream()
        			.filter(HealthProfessional::isAuthorizationValid)
        			.sorted(Comparator.comparing(HealthProfessional::getAuthorizationDate).reversed())
        			.map(h -> toAuthorizationCode(h))
        			.collect(Collectors.toList());
        	
        	// all authCodes are invalid
        	if (sortedAndValidAuthorizationCodes.size() == 0) {
        		return;
        	}
        	
        	if (person.getAuthorizationCodes() == null || person.getAuthorizationCodes().size() == 0) {
        		if (person.getAuthorizationCodes() == null) {
        			person.setAuthorizationCodes(new ArrayList<>());
        		}
        		
        		// flag the first as prime
        		sortedAndValidAuthorizationCodes.get(0).setPrime(true);

        		for (AuthorizationCode authorizationCode : sortedAndValidAuthorizationCodes) {
        			PersonAuthorizationCodeMapping mapping = new PersonAuthorizationCodeMapping();
        			mapping.setPerson(person);
        			mapping.setAuthorizationCode(authorizationCode);
        			
        			person.getAuthorizationCodes().add(mapping);
        		}
        		
        		changes = true;
        	}
        	else {

        		// find those to remove
        		for (Iterator<PersonAuthorizationCodeMapping> iterator = person.getAuthorizationCodes().iterator(); iterator.hasNext();) {
					PersonAuthorizationCodeMapping personCode = iterator.next();
					
					if (sortedAndValidAuthorizationCodes.stream().noneMatch(a -> Objects.equals(a.getCode(), personCode.getAuthorizationCode().getCode()))) {
						iterator.remove();
						changes = true;
					}
				}
        		
        		// find those to add
        		for (AuthorizationCode authorizationCode : sortedAndValidAuthorizationCodes) {
        			if (person.getAuthorizationCodes().stream().noneMatch(pa -> Objects.equals(pa.getAuthorizationCode().getCode(), authorizationCode.getCode()))) {
            			PersonAuthorizationCodeMapping mapping = new PersonAuthorizationCodeMapping();
            			mapping.setPerson(person);
            			mapping.setAuthorizationCode(authorizationCode);
            			
            			person.getAuthorizationCodes().add(mapping);

            			changes = true;
        			}
        		}
        	}
        }

        if (changes) {
        	log.info("Updating authorization codes on " + PersonService.getName(person) + " / " + person.getUuid());
			personService.save(person);
        }
    }

	private AuthorizationCode toAuthorizationCode(HealthProfessional healthProfessional) {
		AuthorizationCode authorizationCode = new AuthorizationCode();
		authorizationCode.setCode(healthProfessional.getAuthorizationID());
		authorizationCode.setName(healthProfessional.getProfessionCodeName());
		authorizationCode.setPrime(false);
		
		return authorizationCode;
	}
	
    private static HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "text/xml; charset=utf-8");
        headers.add("Accept", "text/xml");

        return headers;
    }
}
