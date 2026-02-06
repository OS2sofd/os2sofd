package dk.digitalidentity.sofd.controller.rest;

import java.util.List;
import java.util.UUID;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.controller.rest.model.OrganisationDTO;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Organisation;
import dk.digitalidentity.sofd.security.RequireControllerWriteAccess;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.OrganisationService;

@RestController
@RequireReadAccess
public class OrganisationRestController {

	@Autowired
	private OrganisationService organisationService;
	
	@Autowired
	private OrgUnitService orgUnitService;

	@RequireControllerWriteAccess
	@ResponseBody
	@PostMapping(value = "/rest/organisation/update")
	public HttpEntity<String> createOrUpdateOrganisation(@RequestBody @Valid OrganisationDTO organisationDTO) throws Exception {
		Organisation organisation = null;

		if (organisationDTO.getId() == 0) {
			if (!organisationDTO.getShortName().isEmpty() && organisationService.getByShortName(organisationDTO.getShortName()) != null) {
				return new ResponseEntity<>("Organisation med kortnavn " + organisationDTO.getShortName() + " eksisterer i forvejen", HttpStatus.BAD_REQUEST);
			}

			organisation = new Organisation();
			organisation.setShortName(organisationDTO.getShortName());
		}
		else {
			organisation = organisationService.getById(organisationDTO.getId());

			if (organisation == null) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
		}

		organisation.setName(organisationDTO.getName());
		organisation.setShortName(organisationDTO.getShortName());
		organisation.setDescription(organisationDTO.getDescription());

		organisation = organisationService.save(organisation);

		if (organisationDTO.getId() == 0) {
			OrgUnit orgUnit = new OrgUnit();
			orgUnit.setBelongsTo(organisation);
			orgUnit.setMaster("SOFD");
			orgUnit.setMasterId(UUID.randomUUID().toString());
			orgUnit.setSourceName(organisation.getName());
			orgUnit.setShortname(organisation.getShortName());
			orgUnit.setType(orgUnitService.getDepartmentType());
			orgUnit.setUuid(UUID.randomUUID().toString());
			orgUnitService.save(orgUnit);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@ResponseBody
	@GetMapping(value = "/rest/organisation/get-orgs")
	public HttpEntity<List<OrganisationDTO>> getOrgs() {

		var result = organisationService.getAll().stream().map(OrganisationDTO::new).toList();
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
}
