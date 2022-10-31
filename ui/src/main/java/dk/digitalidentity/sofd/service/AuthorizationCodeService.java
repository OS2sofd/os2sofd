package dk.digitalidentity.sofd.service;

import java.security.SecureRandom;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.service.model.ArrayOfHealthProfessional;
import dk.digitalidentity.sofd.service.model.HealthProfessional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AuthorizationCodeService {

    @Autowired
    private PersonService personService;

    @Transactional
	public void syncAll() {
    	SecureRandom random = new SecureRandom();

		for (Person person : personService.getActive()) {
			// those who already have a code are only updated every 4th run to avoid spamming the service
			if (StringUtils.hasLength(person.getAuthorizationCode())) {
				if (random.nextInt(4) != 2) {
					continue;
				}
			}

			syncAuthorizationCode(person);
		}
	}
    
    public void syncAuthorizationCode(Person person) {
    	String name = PersonService.getName(person);
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
        String url = "http://autregwebservice.sst.dk/autregservice.asmx/GetHealthProfessionals?name=" + name + "&authorizationId=\"\"&birthdayFrom=" + date + "&birthdayTo=" + date + "&authorizationDateFrom=1950-01-01&authorizationDateTo=2030-01-01&professionGroup=\"\"&specialityName=\"\"&authorizationStatus=Valid&seventyFiveYearsRule=NotSpecified";

        HttpEntity<String> request = new HttpEntity<>(headers);
        
        ArrayOfHealthProfessional authResponse = null;
        String newAuthorizationCode = person.getAuthorizationCode();

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
            	String body = response.getBody();
            	if (body == null) {
            		log.warn("Failed to fetch authorization code for substitute with uuid " + person.getUuid() + ". Body is null.");
            		return;
            	}
            	
            	XmlMapper xmlMapper = XmlMapper.builder()
												.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
												.defaultUseWrapper(false)
												.build();
    			try {
					authResponse = xmlMapper.readValue(body, ArrayOfHealthProfessional.class);
				}
    			catch (Exception ex) {
					log.error("Failed to fetch authorization code for substitute with uuid " + person.getUuid() + ". Could not parse xml", ex);
					return;
				}
            }
        }
        catch (HttpClientErrorException ex) {
            log.warn("Failed to fetch authorization code for substitute with uuid " + person.getUuid() + ". Error: " + ex.getResponseBodyAsString(), ex);
            return;
        }

        if (authResponse.getHealthProfessionals() == null || authResponse.getHealthProfessionals().isEmpty()) {
        	log.info("Failed to fetch authorization code for substitute with uuid " + person.getUuid() + ". Returned no health professionals");
        	return;
        }
        else {
            HealthProfessional healthProfessional = authResponse.getHealthProfessionals().get(0);
            if (healthProfessional.isAuthorizationValid()) {
            	newAuthorizationCode = healthProfessional.getAuthorizationID();
            }
        }

        if ((person.getAuthorizationCode() == null && newAuthorizationCode != null) || (person.getAuthorizationCode() != null && !person.getAuthorizationCode().equals(newAuthorizationCode))) {
        	person.setAuthorizationCode(newAuthorizationCode);

        	personService.save(person);
        }
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "text/xml; charset=utf-8");
        headers.add("Accept", "text/xml");

        return headers;
    }
}
