package dk.digitalidentity.sofd.controller.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.SubstituteOrgUnitAssignment;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.EventType;
import dk.digitalidentity.sofd.log.AuditLogger;
import dk.digitalidentity.sofd.service.SubstituteOrgUnitAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.controller.rest.model.SubstituteAssignmentAddDTO;
import dk.digitalidentity.sofd.controller.rest.model.SubstituteContextDTO;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SubstituteAssignment;
import dk.digitalidentity.sofd.dao.model.SubstituteAssignmentOrgUnitMapping;
import dk.digitalidentity.sofd.dao.model.SubstituteContext;
import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.security.RequireControllerWriteAccess;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SubstituteAssignmentService;
import dk.digitalidentity.sofd.service.SubstituteContextService;
import dk.digitalidentity.sofd.telephony.controller.rest.dto.AutoCompleteResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequireControllerWriteAccess
@RestController
public class SubstituteRestController {

	@Autowired
	private SubstituteContextService substituteContextService;

	@Autowired
	private SubstituteAssignmentService substituteAssignmentService;

	@Autowired
	private PersonService personService;

	@Autowired
	private OrgUnitService orgUnitService;

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private SubstituteOrgUnitAssignmentService substituteOrgUnitAssignmentService;

	@Autowired
	private AuditLogger auditLogger;

	@RequireAdminAccess
	@ResponseBody
	@PostMapping(value = "/rest/substituteContext/update")
	public HttpEntity<?> createSubstituteContext(@RequestBody SubstituteContextDTO dto) {
		SubstituteContext subContext;

		if (dto.getId() == 0) {
			subContext = new SubstituteContext();
			subContext.setName(dto.getName());
			subContext.setIdentifier(dto.getIdentifier());
			subContext.setSupportsConstraints(dto.isSupportsConstraints());
		}
		else {

			// editing is only allowed when orgUnitSubstitute is enabled
			if (!configuration.getModules().getOrgUnitSubstitute().isEnabled()) {
				return ResponseEntity.badRequest().build();
			}

			subContext = substituteContextService.getById(dto.getId());
			if (subContext == null) {
				return ResponseEntity.badRequest().build();
			}

			// if we go from assignableToOrgUnit to not assignableToOrgUnit we delete all substituteOrgUnitAssignments with this context
			if (subContext.isAssignableToOrgUnit() && !dto.isAssignableToOrgUnit()) {
				substituteOrgUnitAssignmentService.deleteAllByContext(subContext);
			}
		}

		subContext.setAssignableToOrgUnit(dto.isAssignableToOrgUnit());
		subContext.setInheritOrgUnitAssignments(dto.isInherit());

		substituteContextService.save(subContext);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireAdminAccess
	@ResponseBody
	@DeleteMapping(value = "/rest/substituteContext/delete")
	public HttpEntity<?> deleteSubstituteContexts(@RequestBody Long id) {
		SubstituteContext subContext = substituteContextService.getById(id);
		if (subContext == null) {
			return ResponseEntity.badRequest().build();
		}

		substituteContextService.delete(subContext);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@ResponseBody
	@DeleteMapping(value = "/rest/substituteOrgUnitAssignment/delete/{id}")
	public HttpEntity<?> deleteSubstituteOrgUnitAssignemnt(@PathVariable("id") Long id) {
		if (!configuration.getModules().getOrgUnitSubstitute().isEnabled()) {
			return ResponseEntity.badRequest().build();
		}
		SubstituteOrgUnitAssignment assignment = substituteOrgUnitAssignmentService.getById(id);
		if (assignment == null) {
			return ResponseEntity.badRequest().build();
		}

		Person substitute = assignment.getSubstitute();
		var message = PersonService.getName(substitute)  + " (" + substitute.getCprMaskSuffix() + ") slettet som stedfortræder i " + assignment.getOrgUnit().getEntityName() + " med stedfortræderkontekst " + assignment.getContext().getName() + ".";
		log.info(message);
		auditLogger.log(String.valueOf(assignment.getId()), EntityType.SUBSTITUTE_ORGUNIT_ASSIGNMENT, EventType.DELETE, assignment.getSubstitute().getEntityName(), message);
		substituteOrgUnitAssignmentService.delete(assignment);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@ResponseBody
	@DeleteMapping(value = "/rest/substituteAssignment/delete/{id}")
	public HttpEntity<?> deleteSubstituteAssignemnt(@PathVariable("id") Long id) {
		SubstituteAssignment assignment = substituteAssignmentService.getById(id);
		if (assignment == null) {
			return ResponseEntity.badRequest().build();
		}

		Person substitute = assignment.getSubstitute();
		Person person = assignment.getPerson();
		var message = PersonService.getName(substitute)  + " (" + substitute.getCprMaskSuffix() + ") slettet som stedfortræder for " + PersonService.getName(person)  + " (" + person.getCprMaskSuffix() + ") med stedfortræderkontekst " + assignment.getContext().getName() + ".";
		log.info(message);
		auditLogger.log(String.valueOf(assignment.getId()), EntityType.SUBSTITUTE_ASSIGNMENT, EventType.DELETE, assignment.getSubstitute().getEntityName(), message);
		substituteAssignmentService.delete(assignment);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@ResponseBody
	@GetMapping(value = "/rest/substituteAssignment/search/person/{uuid}")
	public ResponseEntity<?> searchPerson(@PathVariable("uuid") String uuid, @RequestParam("query") String term) {
		AutoCompleteResult result = personService.substituteSearchPerson(term, uuid, null);
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@ResponseBody
	@PostMapping(value = "/rest/substituteAssignment/add")
	public HttpEntity<?> createSubstituteAssignment(@RequestBody SubstituteAssignmentAddDTO dto) {
		SubstituteAssignment ass = new SubstituteAssignment();

		Person person = personService.getByUuid(dto.getPerson());
		if (person == null) {
			log.warn("Person not found for uuid: " + dto.getPerson());
			return ResponseEntity.badRequest().build();
		}

		Person substitute = personService.getByUuid(dto.getSubstitute());
		if (substitute == null) {
			log.warn("Person(Substitute) not found for uuid: " + dto.getPerson());
			return ResponseEntity.notFound().build();
		}

		SubstituteContext selectedContext = substituteContextService.getById(dto.getContext());
		if (selectedContext == null) {
			log.warn("SubstituteContext not found for id: " + dto.getContext());
			return ResponseEntity.badRequest().build();
		}
		else {
			ass.setContext(selectedContext);
		}

		List<SubstituteAssignmentOrgUnitMapping> constraintMappings = new ArrayList<>();
		if (selectedContext.isSupportsConstraints() && dto.getConstraint() != null) {
			List<OrgUnit> orgUnits = orgUnitService.getByUuid(Arrays.asList(dto.getConstraint()));

			for (OrgUnit orgUnit : orgUnits) {
				SubstituteAssignmentOrgUnitMapping cm = new SubstituteAssignmentOrgUnitMapping();
				cm.setSubstituteAssignment(ass);
				cm.setOrgUnit(orgUnit);
				constraintMappings.add(cm);
			}
		}
		
		ass.setConstraintMappings(constraintMappings);
		ass.setPerson(person);
		ass.setSubstitute(substitute);

		var assignment = substituteAssignmentService.save(ass);
		String message = PersonService.getName(substitute)  + " (" + substitute.getCprMaskSuffix() + ") opsat som stedfortræder for " + PersonService.getName(person)  + " (" + person.getCprMaskSuffix() + ") med stedfortræderkontekst " + assignment.getContext().getName() + ".";
		log.info(message);
		auditLogger.log(String.valueOf(assignment.getId()), EntityType.SUBSTITUTE_ASSIGNMENT, EventType.SAVE, assignment.getSubstitute().getEntityName(), message);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	record SubstituteOrgUnitAssignmentAddDTO(String substitute, String orgUnit, long context) {}
	@ResponseBody
	@PostMapping(value = "/rest/substituteOrgUnitAssignment/add")
	public HttpEntity<?> createSubstituteOrgUnitAssignment(@RequestBody SubstituteOrgUnitAssignmentAddDTO dto) {
		if (!configuration.getModules().getOrgUnitSubstitute().isEnabled()) {
			return ResponseEntity.badRequest().build();
		}

		OrgUnit ou = orgUnitService.getByUuid(dto.orgUnit());
		if (ou == null) {
			log.warn("OrgUnit not found for uuid: " + dto.orgUnit());
			return ResponseEntity.badRequest().build();
		}

		Person substitute = personService.getByUuid(dto.substitute());
		if (substitute == null) {
			log.warn("Person(Substitute) not found for uuid: " + dto.substitute());
			return ResponseEntity.notFound().build();
		}

		SubstituteContext selectedContext = substituteContextService.getById(dto.context());
		if (selectedContext == null) {
			log.warn("SubstituteContext not found for id: " + dto.context());
			return ResponseEntity.badRequest().build();
		}

		SubstituteOrgUnitAssignment substituteOrgUnitAssignment = new SubstituteOrgUnitAssignment();
		substituteOrgUnitAssignment.setContext(selectedContext);
		substituteOrgUnitAssignment.setOrgUnit(ou);
		substituteOrgUnitAssignment.setSubstitute(substitute);

		var assignment = substituteOrgUnitAssignmentService.save(substituteOrgUnitAssignment);
		var message = PersonService.getName(substitute)  + " (" + substitute.getCprMaskSuffix() + ") opsat som stedfortræder i " + assignment.getOrgUnit().getEntityName() + " med stedfortræderkontekst " + assignment.getContext().getName() + ".";
		log.info(message);
		auditLogger.log(String.valueOf(assignment.getId()), EntityType.SUBSTITUTE_ORGUNIT_ASSIGNMENT, EventType.SAVE, assignment.getSubstitute().getEntityName(), message);


		return new ResponseEntity<>(HttpStatus.OK);
	}
}
