package dk.digitalidentity.sofd.controller.api;

import java.util.ArrayList;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.controller.api.dto.CreateAffiliationApiDTO;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.EventType;
import dk.digitalidentity.sofd.log.AuditLogger;
import dk.digitalidentity.sofd.security.RequireApiWriteAccess;
import dk.digitalidentity.sofd.security.RequireDaoWriteAccess;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.PersonService;

@RequireDaoWriteAccess
@RestController
public class CreateAffiliationApi {

	@Autowired
	private OrgUnitService orgUnitService;
	
	@Autowired
	private PersonService personService;
	
	@Autowired
	private AuditLogger auditLogger;

	@RequireApiWriteAccess
	@PostMapping("/api/createAffiliation")
	public ResponseEntity<String> postManagedTitles(@RequestBody CreateAffiliationApiDTO dto) throws Exception {
		OrgUnit orgUnit = orgUnitService.getByUuid(dto.getOrgUnitUuid());
		if (orgUnit == null) {
			return new ResponseEntity<>("Enheden findes ikke: " + dto.getOrgUnitUuid(), HttpStatus.NOT_FOUND);
		}
		
		if (!validCpr(dto.getCpr())) {
			return new ResponseEntity<>("Ugyldigt cpr-nummer", HttpStatus.NOT_FOUND);
		}
		
		if (dto.getStartDate() != null && dto.getStopDate() != null && !dto.getStartDate().before(dto.getStopDate())) {
			return new ResponseEntity<>("Startdatoen skal være før stopdatoen", HttpStatus.NOT_FOUND);
		}
		
		Person person = personService.findByCpr(dto.getCpr());
		if (person == null) {
			person = new Person();
			person.setCpr(dto.getCpr());
			person.setUuid(UUID.randomUUID().toString());
			person.setMaster("SOFD");
			person.setFirstname(dto.getFirstname());
			person.setSurname(dto.getSurname());
		}
		
		Affiliation affiliation = new Affiliation();
		affiliation.setUuid(UUID.randomUUID().toString());
		affiliation.setMaster("SOFD");
		affiliation.setMasterId(UUID.randomUUID().toString());
		affiliation.setOrgUnit(orgUnit);
		affiliation.setPerson(person);
		affiliation.setPositionName((StringUtils.hasLength(dto.getPositionName())) ? dto.getPositionName().trim() : "Ukendt");
		affiliation.setAffiliationType(dto.getAffiliationType());

		affiliation.setStopDate(dto.getStopDate());
		affiliation.setStartDate(dto.getStartDate());
		
		if (person.getAffiliations() == null) {
			person.setAffiliations(new ArrayList<>());
		}
		person.getAffiliations().add(affiliation);
		
		person = personService.save(person);
		var sb = new StringBuilder();
		sb.append("Tilhørsforhold oprettet via API. ");
		sb.append(person.getEntityLogInfo()).append(", ");
		sb.append("Startdato: ").append(affiliation.getStartDate()).append(", ");
		sb.append("Slutdato: ").append(affiliation.getStopDate()).append(", ");
		sb.append("Stillingsbetegnelse: ").append(affiliation.getPositionName()).append(", ");
		sb.append("Enhed: ").append(orgUnit.getName());
		auditLogger.log(person.getUuid(), EntityType.PERSON, EventType.PERSON_CREATE, PersonService.getName(person), sb.toString());

		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	private boolean validCpr(String cpr) {
		return cpr != null && cpr.matches("^\\d{10}$");
	}
}
