package dk.digitalidentity.sofd.controller.rest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Workplace;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderDeactivateAndDeleteRule;
import dk.digitalidentity.sofd.dao.model.enums.AffiliationType;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.EventType;
import dk.digitalidentity.sofd.dao.model.mapping.AffiliationPrimaryKleMapping;
import dk.digitalidentity.sofd.dao.model.mapping.AffiliationSecondaryKleMapping;
import dk.digitalidentity.sofd.log.AuditLogger;
import dk.digitalidentity.sofd.security.RequireControllerWriteAccess;
import dk.digitalidentity.sofd.security.RequirePersonCreaterOrControllerWriteAccess;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.PersonService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class AffiliationRestController {

	@Autowired
	private AuditLogger auditLogger;
	
	@Autowired
	private OrgUnitService orgUnitService;

	@Autowired
	private PersonService personService;

	@RequireControllerWriteAccess
	@PostMapping(value = "/rest/affil/update/kle")
	@ResponseBody
	public HttpEntity<String> updateKle(@RequestHeader("uuid") String uuid, @RequestHeader("type") String type, @RequestBody List<String> codes) {
		var person = personService.findByAffiliationUuid(uuid);
		if( person == null ) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		Affiliation affiliation = person.getAffiliations().stream().filter(a -> a.getUuid().equalsIgnoreCase(uuid)).findFirst().get();

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
		
		personService.save(person);

		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@RequirePersonCreaterOrControllerWriteAccess
	@PostMapping(value = "/rest/affil/delete/{uuid}")
	@ResponseBody
	public HttpEntity<String> delete(@PathVariable("uuid") String uuid) {
		var person = personService.findByAffiliationUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		var affiliation = person.getAffiliations().stream().filter(a -> a.getUuid().equalsIgnoreCase(uuid)).findFirst().get();

		if (!affiliation.getMaster().equals("SOFD")) {
			log.warn("Affiliation with uuid " + uuid + " is not owned by SOFD!");
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		
		Set<String> constraintOUs = SecurityUtil.getOrgUnitUuidsConstraintFromPersonCreaterRole();
		if (constraintOUs.size() > 0 && !constraintOUs.contains(affiliation.getCalculatedOrgUnit().getUuid())) { // TODO maybe filter on both
			log.warn("Affiliation with uuid " + uuid + " can not be edited by this user");
			return new ResponseEntity<>("Affiliation with uuid " + uuid + " can not be edited by this user", HttpStatus.BAD_REQUEST);
		}

		person.getAffiliations().remove(affiliation);
		personService.save(person);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	record AffiliationEditRecord(String positionDisplayName, String alternativeOrgUnit, String positionName,
								 String orgUnitUuid, Date startDate, Date stopDate, AffiliationType affiliationType,
								 String internalReference, String vendor, boolean transferFKOrg,
								 AccountOrderDeactivateAndDeleteRule deactivateAndDeleteRule) {}
	
	@RequirePersonCreaterOrControllerWriteAccess
	@PostMapping(value = "/rest/affil/core/edit/{uuid}")
	@ResponseBody
	public HttpEntity<String> edit(@PathVariable("uuid") String uuid, @RequestBody AffiliationEditRecord body) {
		var person = personService.findByAffiliationUuid(uuid);
		if( person == null ) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		Affiliation affiliation = person.getAffiliations().stream().filter(a -> a.getUuid().equalsIgnoreCase(uuid)).findFirst().get();

		if (StringUtils.hasLength(body.alternativeOrgUnit)) {
			OrgUnit alternativeOrgUnit = orgUnitService.getByUuid(body.alternativeOrgUnit);

			if (alternativeOrgUnit == null) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}

			affiliation.setAlternativeOrgUnit(alternativeOrgUnit);
		}
		else {
			affiliation.setAlternativeOrgUnit(null);
		}
		
		//checks for null startDate, tries to fallback on previous validDate if possible, otherwise sends a bad request.
		if (body.startDate == null && affiliation.getStartDate() == null) {
			log.warn("Blank affiliation startDate not allowed. No fallback, so Bad Request sent to user. Uuid: " + uuid);
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		else if (body.startDate == null && affiliation.getStartDate() != null) {
			log.info("Blank affiliation startDate not allowed. Previous startDate used as fallback. Uuid: " + uuid);
		}
		else {
			affiliation.setStartDate(body.startDate);
		}

		affiliation.setPositionDisplayName(body.positionDisplayName);
		affiliation.setPositionName(body.positionName);
		OrgUnit orgUnit = OrgUnitService.getInstance().getByUuid(body.orgUnitUuid);
		if(Objects.equals(null, orgUnit)) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		affiliation.setOrgUnit(orgUnit);
		affiliation.setStopDate(body.stopDate);
		affiliation.setAffiliationType(body.affiliationType);
		affiliation.setInternalReference(StringUtils.hasLength(body.internalReference) ? body.internalReference : null);
		affiliation.setDoNotTransferToFkOrg(body.transferFKOrg);
		affiliation.setVendor(StringUtils.hasLength(body.vendor) ? body.vendor : null);
		affiliation.setDeactivateAndDeleteRule(body.deactivateAndDeleteRule);


		String message = "Tilhørsforhold " + affiliation.getPositionName() + " i " + affiliation.getCalculatedOrgUnit().getName() + " ændret på " + PersonService.getName(person) + " (" + person.getCprMaskSuffix() + ").";
		auditLogger.log(person.getUuid(), EntityType.PERSON, EventType.AFFILIATION_CREATED, PersonService.getName(person), message);
		personService.save(person);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	record WorkplaceDTO(String affiliationUuid, LocalDate startDate, LocalDate stopDate, String orgUnitUuid) {}
	@RequirePersonCreaterOrControllerWriteAccess
	@PostMapping(value = "/rest/affil/workplace/new")
	@ResponseBody
	public HttpEntity<String> delete(@RequestBody WorkplaceDTO dto) {
		var person = personService.findByAffiliationUuid(dto.affiliationUuid);
		if( person == null ) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		Affiliation affiliation = person.getAffiliations().stream().filter(a -> a.getUuid().equalsIgnoreCase(dto.affiliationUuid)).findFirst().get();

		OrgUnit orgUnit = orgUnitService.getByUuid(dto.orgUnitUuid);
		if (orgUnit == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		if (dto.startDate == null || dto.stopDate == null) {
			return new ResponseEntity<>("Der skal angives en start- og stopdato", HttpStatus.BAD_REQUEST);
		}

		if (dto.startDate.isAfter(dto.stopDate)) {
			return new ResponseEntity<>("Startdatoen skal være efter stopdatoen", HttpStatus.BAD_REQUEST);
		}

		if (isOverlap(dto.startDate, dto.stopDate, affiliation.getWorkplaces())) {
			return new ResponseEntity<>("Den valgte periode for arbejdsstedet overlapper med perioden for en af de andre arbejdssteder", HttpStatus.BAD_REQUEST);
		}

		Workplace workplace = new Workplace();
		workplace.setStartDate(dto.startDate);
		workplace.setStopDate(dto.stopDate);
		workplace.setAffiliation(affiliation);
		workplace.setOrgUnit(orgUnit);

		if (affiliation.getWorkplaces() == null) {
			affiliation.setWorkplaces(new ArrayList<>());
		}

		affiliation.getWorkplaces().add(workplace);

		personService.save(person);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	record WorkplaceDeleteDTO(String affiliationUuid, long workplaceId) {}
	@RequirePersonCreaterOrControllerWriteAccess
	@PostMapping(value = "/rest/affil/workplace/delete")
	@ResponseBody
	public HttpEntity<String> delete(@RequestBody WorkplaceDeleteDTO dto) {
		var person = personService.findByAffiliationUuid(dto.affiliationUuid);
		if( person == null ) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		Affiliation affiliation = person.getAffiliations().stream().filter(a -> a.getUuid().equalsIgnoreCase(dto.affiliationUuid)).findFirst().get();

		affiliation.getWorkplaces().removeIf(w -> w.getId() == dto.workplaceId);
		personService.save(person);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	public boolean isOverlap(LocalDate startDate, LocalDate stopDate, List<Workplace> otherWorkplaces) {
		for (Workplace workplace : otherWorkplaces) {
			LocalDate otherStartDate = workplace.getStartDate();
			LocalDate otherStopDate = workplace.getStopDate();

			if (!(stopDate.isBefore(otherStartDate) || startDate.isAfter(otherStopDate))) {
				return true;
			}
		}
		return false;
	}
}
