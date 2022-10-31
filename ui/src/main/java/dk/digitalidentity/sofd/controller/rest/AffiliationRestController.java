package dk.digitalidentity.sofd.controller.rest;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import dk.digitalidentity.sofd.security.RequireAdminAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.mapping.AffiliationPrimaryKleMapping;
import dk.digitalidentity.sofd.dao.model.mapping.AffiliationSecondaryKleMapping;
import dk.digitalidentity.sofd.security.RequireControllerWriteAccess;
import dk.digitalidentity.sofd.security.RequirePersonCreaterOrControllerWriteAccess;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.AffiliationService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class AffiliationRestController {

	@Autowired
	private AffiliationService affiliationService;

	@RequireControllerWriteAccess
	@PostMapping(value = "/rest/affil/update/kle")
	@ResponseBody
	public HttpEntity<String> updateKle(@RequestHeader("uuid") String uuid, @RequestHeader("type") String type, @RequestBody List<String> codes) {
		Affiliation affiliation = affiliationService.findByUuid(uuid);
		if (affiliation == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		if ("KlePrimary".equals(type)) {
			List<AffiliationPrimaryKleMapping> existingKles = affiliation.getKlePrimary();
			
			// To remove
			for (Iterator<AffiliationPrimaryKleMapping> iterator = existingKles.iterator(); iterator.hasNext();) {
				AffiliationPrimaryKleMapping existingKle = iterator.next();

				if (!codes.contains(existingKle.getKleValue())) {
					iterator.remove();
				}
			}
			
			// To add
			Set<String> existingCodes = existingKles.stream().map(k -> k.getKleValue()).collect(Collectors.toSet());
			for (String code : codes) {
				if (!existingCodes.contains(code)) {
					AffiliationPrimaryKleMapping mapping = new AffiliationPrimaryKleMapping();
					mapping.setAffiliation(affiliation);
					mapping.setKleValue(code);

					affiliation.getKlePrimary().add(mapping);
				}
			}
		}
		else if ("KleSecondary".equals(type)) {
			List<AffiliationSecondaryKleMapping> existingKles = affiliation.getKleSecondary();
			
			// To remove
			for (Iterator<AffiliationSecondaryKleMapping> iterator = existingKles.iterator(); iterator.hasNext();) {
				AffiliationSecondaryKleMapping existingKle = iterator.next();

				if (!codes.contains(existingKle.getKleValue())) {
					iterator.remove();
				}
			}
			
			// To add
			Set<String> existingCodes = existingKles.stream().map(k -> k.getKleValue()).collect(Collectors.toSet());
			for (String code : codes) {
				if (!existingCodes.contains(code)) {
					AffiliationSecondaryKleMapping mapping = new AffiliationSecondaryKleMapping();
					mapping.setAffiliation(affiliation);
					mapping.setKleValue(code);

					affiliation.getKleSecondary().add(mapping);
				}
			}
		}
		else {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		
		affiliationService.save(affiliation);

		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@RequirePersonCreaterOrControllerWriteAccess
	@PostMapping(value = "/rest/affil/delete/{uuid}")
	@ResponseBody
	public HttpEntity<String> delete(@PathVariable("uuid") String uuid) {
		Affiliation affiliation = affiliationService.findByUuid(uuid);
		if (affiliation == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		if (!affiliation.getMaster().equals("SOFD")) {
			log.warn("Affiliation with uuid " + uuid + " is not owned by SOFD!");
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		
		Set<String> constraintOUs = SecurityUtil.getOrgUnitUuidsFromConstraint();
		if (constraintOUs.size() > 0 && !constraintOUs.contains(affiliation.getOrgUnit().getUuid())) {
			log.warn("Affiliation with uuid " + uuid + " can not be edited by this user");
			return new ResponseEntity<>("Affiliation with uuid " + uuid + " can not be edited by this user", HttpStatus.BAD_REQUEST);
		}

		affiliationService.delete(affiliation);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireAdminAccess
	@PostMapping(value = "/rest/affil/core/edit/{uuid}")
	@ResponseBody
	public HttpEntity<String> delete(@PathVariable("uuid") String uuid, @RequestBody String positionDisplayName) {
		Affiliation affiliation = affiliationService.findByUuid(uuid);
		if (affiliation == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		affiliation.setPositionDisplayName(positionDisplayName);
		affiliationService.save(affiliation);

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
