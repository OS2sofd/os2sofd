package dk.digitalidentity.sofd.controller.rest;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.dao.model.ContactPlace;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.mapping.ContactPlaceKleMapping;
import dk.digitalidentity.sofd.dao.model.mapping.ContactPlaceOrgUnitMapping;
import dk.digitalidentity.sofd.security.RequireControllerWriteAccess;
import dk.digitalidentity.sofd.service.ContactPlaceService;
import dk.digitalidentity.sofd.service.KleService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import lombok.extern.slf4j.Slf4j;

@RequireControllerWriteAccess
@Slf4j
@RestController
public class ContactPlacesRestController {
	
	@Autowired
	private ContactPlaceService contactPlaceService;
	
	@Autowired
	private OrgUnitService orgUnitService;

	@Autowired
	private KleService kleService;
	
	@PutMapping("/rest/contactplaces/{id}/addUser/{uuid}")
	@ResponseBody
	public ResponseEntity<String> addOrgUnitToContactPlace(@PathVariable("id") long id, @PathVariable("uuid") String uuid) {
		ContactPlace contactPlace = contactPlaceService.findById(id);
		if (contactPlace == null) {
			log.warn("Requested ContactPlace with ID:" + id + " not found.");
			return new ResponseEntity<>("Henvendelsesstedet findes ikke", HttpStatus.NOT_FOUND);
		}

		if (contactPlace.getContactPlace().getUuid().equals(uuid)) {
			log.warn("Cannot assign OrgUnit: " + uuid + " to contactPlace with the same OrgUnit.");
			return new ResponseEntity<>("En enhed kan ikke være henvendelsessted for sig selv", HttpStatus.BAD_REQUEST);
		}
		
		OrgUnit user = orgUnitService.getByUuid(uuid);
		if (user == null) {
			log.warn("Requested OrgUnit with UUID:" + uuid + " not found.");
			return new ResponseEntity<>("Den valgte enhed findes ikke", HttpStatus.NOT_FOUND);
		}

		boolean undeleted = false;
		boolean alreadyExists = false;
		for (ContactPlaceOrgUnitMapping mapping : contactPlace.getUsers()) {
			if (Objects.equals(mapping.getOrgUnit().getUuid(), uuid)) {
				if (mapping.isDeleted()) {
					// just undelete
					undeleted = true;
					mapping.setDeleted(false);
				}
				else {
					alreadyExists = true;
				}
			}
		}
		
		if (alreadyExists) {
			log.warn("Requested OrgUnit with UUID:" + uuid + " is already assigned to ContactPlace: " + contactPlace.getId());
			return new ResponseEntity<>("Enheden er allerede tilføjet som anvender", HttpStatus.BAD_REQUEST);
		}

		if (!undeleted) {
			ContactPlaceOrgUnitMapping userMapping = new ContactPlaceOrgUnitMapping();
			userMapping.setContactPlace(contactPlace);
			userMapping.setOrgUnit(user);
	
			contactPlace.getUsers().add(userMapping);
		}

		contactPlace.setSynchronizedToOrganisation(false);
		contactPlaceService.save(contactPlace);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@DeleteMapping("/rest/contactplaces/{id}/removeUser/{uuid}")
	@ResponseBody
	public ResponseEntity<String> removeOrgUnitFormContactPlace(@PathVariable("id") long id, @PathVariable("uuid") String uuid) {
		ContactPlace contactPlace = contactPlaceService.findById(id);
		if (contactPlace == null) {
			log.warn("Requested ContactPlace with ID:" + id + " not found.");
			return new ResponseEntity<>("Henvendelsesstedet findes ikke", HttpStatus.BAD_REQUEST);
		}
		
		OrgUnit user = orgUnitService.getByUuid(uuid);
		if (user == null) {
			log.warn("Requested OrgUnit with UUID:" + uuid + " not found.");
			return new ResponseEntity<>("Den valgte enhed er ikke en anvender af dette henvendelsessted", HttpStatus.BAD_REQUEST);
		}

		ContactPlaceOrgUnitMapping mapping = contactPlace.getUsers().stream().filter(m -> m.getOrgUnit().getUuid().equals(user.getUuid())).findFirst().orElse(null);
		if (mapping != null) {
			mapping.setDeleted(true);

			contactPlace.setSynchronizedToOrganisation(false);
			contactPlaceService.save(contactPlace);			
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping(value = "/rest/contactplaces/update/kle")
	@ResponseBody
	public HttpEntity<String> updateKle(@RequestHeader("id") long id,@RequestBody List<String> codes) throws Exception {
		ContactPlace contactPlace = contactPlaceService.findById(id);
		if (contactPlace == null) {
			log.warn("Requested ContactPlace with ID:" + id + " not found.");
			return new ResponseEntity<>("Henvendelsesstedet findes ikke", HttpStatus.BAD_REQUEST);
		}

		List<ContactPlaceKleMapping> existingKles = contactPlace.getTasks();
		
		// To remove
		for (Iterator<ContactPlaceKleMapping> iterator = existingKles.iterator(); iterator.hasNext();) {
			ContactPlaceKleMapping existingKle = iterator.next();

			if (!codes.contains(existingKle.getKle().getCode())) {
				iterator.remove();
			}
		}
		
		// To add
		Set<String> existingCodes = existingKles.stream().map(k -> k.getKle().getCode()).collect(Collectors.toSet());
		for (String code : codes) {
			if (!existingCodes.contains(code)) {
				ContactPlaceKleMapping kleMapping = new ContactPlaceKleMapping();
				kleMapping.setContactPlace(contactPlace);
				kleMapping.setKle(kleService.getByCode(code));
				contactPlace.getTasks().add(kleMapping);
			}
		}

		contactPlace.setSynchronizedToOrganisation(false);
		contactPlaceService.save(contactPlace);

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
