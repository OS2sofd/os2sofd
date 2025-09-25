package dk.digitalidentity.sofd.controller.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import dk.digitalidentity.sofd.security.RequireLosAdminAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.OrgUnitType;
import dk.digitalidentity.sofd.service.OrgUnitService;
import lombok.extern.slf4j.Slf4j;

@RequireLosAdminAccess
@RestController
@Slf4j
public class OrgUnitTypeRestController {

	@Autowired
	private OrgUnitService orgUnitService;

	@PostMapping("/rest/orgunittype/edit")
	@ResponseBody
	public ResponseEntity<String> editCategory(@RequestBody OrgUnitType orgUnitType) throws Exception {		
		if (orgUnitType.getValue().length() < 1) {
			log.warn("Requested OrgUnitType with value: " + orgUnitType.getValue() + " has invalid value");
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		boolean edit = false;
		OrgUnitType type = new OrgUnitType();
		if (orgUnitType.getId() > 0) {
			edit = true;
			type = orgUnitService.findTypeById(orgUnitType.getId());

			if (type == null) {
				log.warn("Requested OrgUnitType with ID:" + orgUnitType.getId() + " not found.");
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
		}
		else {
			String key = orgUnitType.getKey().toUpperCase().replace(" ", "_");

			if (key.length() < 0) {
				log.warn("Requested OrgUnitType with key: " + orgUnitType.getKey() + " has invalid key");
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
			
			if (orgUnitService.findTypeByKey(key) != null) {
				log.warn("Requested OrgUnitType with key: " + orgUnitType.getKey() + " already exists");
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			type.setKey(key);
		}
		
		List<OrgUnit> ousWithType = new ArrayList<>();
		String oldExtId = null;
		if (edit) {
			ousWithType = orgUnitService.getByOrgUnitType(type);
			oldExtId = type.getExtId();
		}
		 
		type.setValue(orgUnitType.getValue());
		type.setActive(true);
		type.setExtId(orgUnitType.getExtId());

		orgUnitService.saveType(type);
		
		// if the extId changes or its a new type we should update orgUnits
		if (!Objects.equals(oldExtId, orgUnitType.getExtId())) {
			
			// enforce the changes on the orgUnits that have had the type
			for (OrgUnit ou : ousWithType) {
				ou.setType(null);
				orgUnitService.save(ou);
			}
			
			Long extId = null;
			boolean extIdLong = false;
			try {
				extId = Long.parseLong(type.getExtId());
				extIdLong = true;
			} catch (Exception e) {
			}
			
			if (extIdLong) {
				for (OrgUnit ou : orgUnitService.getByOrgUnitTypeId(extId)) {
					
					// just call save cause the abstractBeforeSaveInterceptor will handle setting the orgUnitType
					orgUnitService.save(ou);
				}
			}
			
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
