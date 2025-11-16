package dk.digitalidentity.sofd.controller.api;

import dk.digitalidentity.sofd.security.RequireDaoWriteAccess;
import dk.digitalidentity.sofd.service.ManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequireDaoWriteAccess
@RestController
public class OrgUnitManagerController {

	@Autowired
	ManagerService managerService;

	// todo: temporary check used from OPUS integration
	@GetMapping("/api/orgunitmanager/isMigrated")
	public ResponseEntity<String> isMigrated() {
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/api/orgunitmanager/externalUpdate")
	public ResponseEntity<String> externalUpdate(@RequestBody List<ManagerService.OrgUnitManagerDto> orgUnitManagers, @RequestParam(defaultValue = "true") boolean fullSync) {
		managerService.importManagers(orgUnitManagers, fullSync);
		return new ResponseEntity<>(HttpStatus.OK);
	}

}
