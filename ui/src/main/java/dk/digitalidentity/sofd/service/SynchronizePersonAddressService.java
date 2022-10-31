package dk.digitalidentity.sofd.service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import dk.digitalidentity.sofd.controller.mvc.dto.CprLookupDTO;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.Post;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SynchronizePersonAddressService {

    @Autowired
    private CprService cprService;

    @Autowired
    private PersonService personService;

    @Transactional
    public void updateAddressesOnAllWithLastCprDigit(String digit) {
        List<Person> activePersons = personService.getActive();
        for (Person person : activePersons) {
        	if (person.getCpr().endsWith(digit)) {
        		updatePersonAddress(person);
        	}
        }
    }

    private void updatePersonAddress(Person person) {
        CprLookupDTO cprLookupDTO = cprService.getByCpr(person.getCpr());
        
        if (cprLookupDTO != null) {
            Post cprPost = Post.builder()
                    .master("SOFD")
                    .masterId(UUID.randomUUID().toString())
                    .prime(true)
                    .addressProtected(cprLookupDTO.isAddressProtected())
                    .city(cprLookupDTO.getCity())
                    .country(cprLookupDTO.getCountry())
                    .localname(cprLookupDTO.getLocalname())
                    .postalCode(cprLookupDTO.getPostalCode())
                    .street(cprLookupDTO.getStreet())
                    .build();
            
            String firstname = cprLookupDTO.getFirstname();
            String surname = cprLookupDTO.getLastname();

            if (!StringUtils.hasLength(cprPost.getStreet())) {
            	log.warn("Skipping updatePersonAddress 'street' cannot be null on Person: " + person.getUuid());
            	return;
            }
            else if (!StringUtils.hasLength(cprPost.getPostalCode())) {
            	log.warn("Skipping updatePersonAddress 'postalCode' cannot be null on Person: " + person.getUuid());
            	return;
            }
            else if (!StringUtils.hasLength(cprPost.getCity())) {
            	log.warn("Skipping updatePersonAddress 'city' cannot be null on Person: " + person.getUuid());
            	return;
            }
            else if (!StringUtils.hasLength(cprPost.getCountry())) {
            	log.warn("Skipping updatePersonAddress 'country' cannot be null on Person: " + person.getUuid());
            	return;
            }

            if (!comparePost(cprPost, person.getRegisteredPostAddress()) ||
            	!firstname.equals(person.getFirstname()) ||
            	!surname.equals(person.getSurname())) {

                personService.setPostAndName(person, cprPost, firstname, surname);
            }
        }
    }
    
    private boolean comparePost(Post post1, Post post2) {
    	if (post1 == null && post2 == null) {
    		return true;
    	}

    	if (post1 == null && post2 != null) {
    		return false;
    	}
    	
    	if (post1 != null && post2 == null) {
    		return false;
    	}
    	
    	// from here, both are non-null

    	if (!Objects.equals(post1.isAddressProtected(), post2.isAddressProtected())) {
    		return false;
    	}
    	
    	if (!Objects.equals(post1.getCity(), post2.getCity())) {
    		return false;
    	}
    	
    	if (!Objects.equals(post1.getCountry(), post2.getCountry())) {
    		return false;
    	}
    	
    	if (!Objects.equals(post1.getLocalname(), post2.getLocalname())) {
    		return false;
    	}
    	
    	if (!Objects.equals(post1.getPostalCode(), post2.getPostalCode())) {
    		return false;
    	}
    	
    	if (!Objects.equals(post1.getStreet(), post2.getStreet())) {
    		return false;
    	}

    	// found no differences that matter
    	return true;
    }
}
