package dk.digitalidentity.sofd.controller.api;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.service.SyncService;
import dk.digitalidentity.sofd.service.model.ADGridAD;
import dk.digitalidentity.sofd.service.model.ADGridOrgUnit;
import dk.digitalidentity.sofd.service.model.ADGridPerson;
import dk.digitalidentity.sofd.service.model.SyncResult;

@RestController
@RequireReadAccess
public class SyncController {

	@Autowired
	private SyncService syncService;

	@GetMapping("/api/sync/persons")
	public SyncResult getPersonChanges(@RequestParam(value = "offset", required = true) Long offset) {
		return syncService.getModificationHistory(offset, EntityType.PERSON.toString());
	}

	@GetMapping("/api/sync/orgunits")
	public SyncResult getOrgUnitsChanges(@RequestParam(value = "offset", required = true) Long offset) {
		return syncService.getModificationHistory(offset, EntityType.ORGUNIT.toString());
	}

	@GetMapping("/api/sync/head")
	public Long getMaxOffset() {
		return syncService.getMaxOffset();
	}

	// TODO: this can only be accessed by someone with WRITE or READ access, those with LIMITED_READ cannot access
	//       this.. is that to big of a limitation (if you have LIMITED_READ, you could use this to access data
	//       that you are not allowed to add... so if we want to give accessed to LIMITED_READ clients, we would
	//       need to duplicate the security logic here

	@GetMapping("/api/sync/adgrid/persons")
	public Collection<ADGridPerson> adGridPersons(@RequestParam(required = false) boolean includeUniloginUsers, @RequestParam(required = false) boolean includeSchoolADUsers) {
		return syncService.getADGridPersons(includeUniloginUsers, includeSchoolADUsers);
	}
	
	@GetMapping("/api/sync/adgrid/allad")
	public Collection<ADGridAD> adGridAllAD() {
		return syncService.getADGridAllAD();
	}
	
	@GetMapping("/api/sync/adgrid/allazure")
	public Collection<ADGridAD> adGridAllAzure() {
		return syncService.getADGridAllAzure();
	}

	@GetMapping("/api/sync/adgrid/orgunits")
	public Collection<ADGridOrgUnit> adGridOrgUnits() {
		return syncService.getADGridOrgUnits();
	}

	@GetMapping("/api/sync/adgrid/opusNoAd")
	public Collection<ADGridAD> adGridOpusNoAd() {
		return syncService.getADGridOpusButNoADAccount();
	}
}
