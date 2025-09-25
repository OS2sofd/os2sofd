package dk.digitalidentity.sofd.controller.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.security.RequireApiWriteAccess;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.PrimeService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequireApiWriteAccess
public class PrimeApi {

    @Autowired
    private PrimeService primeService;

    @Autowired
    private PersonService personService;

	@SuppressWarnings("deprecation")
	@PostMapping("/api/prime/fullSyncPhones")
	public ResponseEntity<?> triggerFullSyncPrimes() {		
		log.info("Full prime update on phones triggered through API");

		List<Person> modified = primeService.fullSyncPhones(personService.getActive());

		if (modified.size() > 0) {
			log.info("Found " + modified.size() + " persons that needed to be updated with prime phones");
			
			// I know - do not use this method - but I have special permissions (BSG ;))
			personService.saveAll(modified);
		}

		return ResponseEntity.ok().build();
	}
}