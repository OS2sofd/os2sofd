package dk.digitalidentity.sofd.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.security.RequireDaoWriteAccess;
import dk.digitalidentity.sofd.task.SynchronizeOrgUnitManagersTask;

@RequireDaoWriteAccess
@RestController
public class OrgUnitManagerController {

	@Autowired
	private SynchronizeOrgUnitManagersTask task;

	@RequestMapping("/api/orgunitmanager/sync")
	public ResponseEntity<String> triggerOrgUnitManagerUpdate() throws Exception {
		task.processChanges();
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
