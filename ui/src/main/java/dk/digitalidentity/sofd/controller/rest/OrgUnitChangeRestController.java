package dk.digitalidentity.sofd.controller.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.config.SessionConstants;
import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.OrgUnitFutureChange;
import dk.digitalidentity.sofd.security.RequireLosAdminAccess;
import dk.digitalidentity.sofd.service.OrgUnitFutureChangesService;
import dk.digitalidentity.sofd.service.model.OUTreeForm;

@RequireLosAdminAccess
@RestController
public class OrgUnitChangeRestController {

	@Autowired
	private OrgUnitFutureChangesService orgUnitfutureChangesService;
	
	@Autowired
	private SofdConfiguration configuration;

	@PostMapping(value = "/rest/orgunit/changes/delete")
	@ResponseBody
	public ResponseEntity<List<OUTreeForm>> deleteOuChanges(@RequestBody List<Long> ids) {
		List<OrgUnitFutureChange> toBeDeleted = new ArrayList<>();
		for (Long id : ids) {
			OrgUnitFutureChange orgUnitFutureChange = orgUnitfutureChangesService.getById(id);

			if (orgUnitFutureChange == null) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			toBeDeleted.add(orgUnitFutureChange);
		}

		for (OrgUnitFutureChange orgUnitFutureChange : toBeDeleted) {
			orgUnitfutureChangesService.delete(orgUnitFutureChange);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping(value = "/rest/orgunit/changes/date")
	@ResponseBody
	public ResponseEntity<Date> getDate(HttpServletRequest request) {
		Date date = (Date) request.getSession().getAttribute(SessionConstants.SESSION_FUTURE_DATE);

		return new ResponseEntity<>(date, HttpStatus.OK);
	}

	@PostMapping(value = "/rest/orgunit/changes/date")
	@ResponseBody
	public ResponseEntity<HttpStatus> setDate(HttpServletRequest request, @RequestBody(required = false) Date date) {
		if (!configuration.getModules().getLos().isEnabled() || !configuration.getModules().getLos().isFutureOrgsEnabled()) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		request.getSession().setAttribute(SessionConstants.SESSION_FUTURE_DATE, date);

		return new ResponseEntity<>(HttpStatus.OK);
	}
}