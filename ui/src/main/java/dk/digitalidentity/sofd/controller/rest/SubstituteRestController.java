package dk.digitalidentity.sofd.controller.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

	@RequireAdminAccess
	@ResponseBody
	@PostMapping(value = "/rest/substituteContext/create")
	public HttpEntity<?> createSubstituteContext(@RequestBody SubstituteContextDTO dto) {
		SubstituteContext subContext = new SubstituteContext();
		subContext.setName(dto.getName());
		subContext.setIdentifier(dto.getIdentifier());
		subContext.setSupportsConstraints(dto.isSupportsConstraints());

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
	@DeleteMapping(value = "/rest/substituteAssignment/delete/{id}")
	public HttpEntity<?> deleteSubstituteAssignemnt(@PathVariable("id") Long id) {
		SubstituteAssignment subAssignment = substituteAssignmentService.getById(id);
		if (subAssignment == null) {
			return ResponseEntity.badRequest().build();
		}

		substituteAssignmentService.delete(subAssignment);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@ResponseBody
	@GetMapping(value = "/rest/substituteAssignment/search/person/{uuid}")
	public ResponseEntity<?> searchPerson(@RequestParam("query") String term, @PathVariable("uuid") String uuid) {
		AutoCompleteResult result = personService.substituteSearchPerson(term, uuid);
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

		substituteAssignmentService.save(ass);

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
