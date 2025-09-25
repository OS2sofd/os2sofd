package dk.digitalidentity.sofd.controller.api.v2;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.controller.api.v2.model.AffiliationApiRecord;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.service.AffiliationService;
import dk.digitalidentity.sofd.service.OrgUnitService;

@RestController
@RequireReadAccess
public class AffiliationApi {

	@Autowired
	private OrgUnitService orgUnitService;

	@GetMapping("/api/v2/affiliations/{master}/{orgUnitUuid}")
	public ResponseEntity<?> getOpusAffiliationsByMasterAndOrgUnitUuid(@PathVariable("master") String master, @PathVariable("orgUnitUuid") String orgUnitUuid) {
		OrgUnit orgUnit = orgUnitService.getByUuid(orgUnitUuid);
		if (orgUnit == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);			
		}

		List<AffiliationApiRecord> affiliations = new ArrayList<>();
		if (orgUnit.getAffiliations() != null && orgUnit.getAffiliations().size() > 0) {
			for (Affiliation affiliation : orgUnit.getAffiliations()) {
				if (!affiliation.getMaster().equals(master)) {
					continue;
				}
				
				if (!AffiliationService.activeAndNotStopped(affiliation)) {
					continue;
				}
				
				affiliations.add(new AffiliationApiRecord(affiliation));
			}
		}

		return new ResponseEntity<>(affiliations, HttpStatus.OK);
	}
}
