package dk.digitalidentity.sofd.controller.api;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.controller.api.dto.OUConstraintDTO;
import dk.digitalidentity.sofd.controller.api.dto.SubstituteAssignmentCreateDTO;
import dk.digitalidentity.sofd.controller.api.dto.SubstituteAssignmentDTO;
import dk.digitalidentity.sofd.controller.api.dto.SubstituteContextDTO;
import dk.digitalidentity.sofd.controller.api.dto.SubstitutePersonDTO;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SubstituteAssignment;
import dk.digitalidentity.sofd.dao.model.SubstituteAssignmentOrgUnitMapping;
import dk.digitalidentity.sofd.dao.model.SubstituteContext;
import dk.digitalidentity.sofd.security.RequireDaoWriteAccess;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SubstituteAssignmentService;
import dk.digitalidentity.sofd.service.SubstituteContextService;
import dk.digitalidentity.sofd.telephony.controller.rest.dto.AutoCompleteResult;

@RequireDaoWriteAccess
@RestController
public class SubstituteApiController {
	
	@Autowired
	private PersonService personService;
	
	@Autowired
	private SubstituteContextService substituteContextService;
	
	@Autowired
	private SubstituteAssignmentService substituteAssignmentService;
	
	@Autowired
	private OrgUnitService orgUnitService;
	
	@GetMapping("/api/substitutes/assignments/{uuid}")
	public ResponseEntity<?> getSubstituteAssignments(@PathVariable String uuid) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>("Person with uuid " + uuid + " was not found", HttpStatus.NOT_FOUND);
		}
		
		List<SubstituteAssignmentDTO> result = new ArrayList<>();
		for (SubstituteAssignment assignment : person.getSubstitutes()) {
			SubstituteAssignmentDTO dto = new SubstituteAssignmentDTO();
			dto.setConstraintOrgUnits(assignment.getConstraintMappings().stream().map(c -> OUConstraintDTO.builder().name(c.getOrgUnit().getName()).uuid(c.getOrgUnit().getUuid()).build()).collect(Collectors.toList()));
			dto.setId(assignment.getId());
			dto.setSubstituteContextId(assignment.getContext().getId());
			dto.setSubstituteContextName(assignment.getContext().getName());
			
			SubstitutePersonDTO substitute = new SubstitutePersonDTO();
			substitute.setName(PersonService.getName(assignment.getSubstitute()));
			substitute.setUuid(assignment.getSubstitute().getUuid());
			dto.setSubstitute(substitute);
			
			result.add(dto);
		}
		
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
	
	@GetMapping("/api/substitutes/contexts")
	public ResponseEntity<?> getSubstituteContexts() {
		List<SubstituteContextDTO> result = new ArrayList<>();
		for (SubstituteContext context : substituteContextService.getAll()) {
			SubstituteContextDTO dto = new SubstituteContextDTO();
			dto.setCanBeDeleted(context.isCanBeDeleted());
			dto.setId(context.getId());
			dto.setIdentifier(context.getIdentifier());
			dto.setName(context.getName());
			dto.setSupportsConstraints(context.isSupportsConstraints());
			
			result.add(dto);
		}
		
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
	
	@GetMapping(value = "/api/substitutes/search/person/{uuid}")
	public ResponseEntity<?> searchPerson(@RequestParam("query") String term, @PathVariable("uuid") String uuid) {
		AutoCompleteResult result = personService.substituteSearchPerson(term, uuid);
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
	
	@GetMapping("/api/substitutes/managedorgunits/{uuid}")
	public ResponseEntity<?> getManagedOUs(@PathVariable String uuid) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>("Person with uuid " + uuid + " was not found", HttpStatus.NOT_FOUND);
		}
		
		List<OUConstraintDTO> managedOrgUnits = orgUnitService.getAllWhereManagerIs(person).stream().map(o -> OUConstraintDTO.builder().name(o.getName()).uuid(o.getUuid()).build()).collect(Collectors.toList());
		
		return new ResponseEntity<>(managedOrgUnits, HttpStatus.OK);
	}
	
	@PostMapping(value = "/api/substitutes/assignments/create")
	public ResponseEntity<?> createSubstituteAssignment(@RequestBody SubstituteAssignmentCreateDTO dto) {
		Person person = personService.getByUuid(dto.getPersonUuid());
		if (person == null) {
			return new ResponseEntity<>("Person with uuid " + dto.getPersonUuid() + " was not found", HttpStatus.NOT_FOUND);
		}
		
		Person substitute = personService.getByUuid(dto.getSubstitutePersonUuid());
		if (substitute == null) {
			return new ResponseEntity<>("Person with uuid " + dto.getSubstitutePersonUuid() + " was not found", HttpStatus.NOT_FOUND);
		}

		SubstituteContext context = substituteContextService.getById(dto.getSubstituteContextId());
		if (context == null) {
			return new ResponseEntity<>("SubstituteContext with id " + dto.getSubstituteContextId() + " was not found", HttpStatus.NOT_FOUND);
		}

		SubstituteAssignment substituteAssignment = new SubstituteAssignment();
		substituteAssignment.setContext(context);
		
		List<SubstituteAssignmentOrgUnitMapping> constraintMappings = new ArrayList<>();
		if (context.isSupportsConstraints() && dto.getConstraintOrgUnitUuids() != null) {
			List<String> managedOrgUnitUuids = orgUnitService.getAllWhereManagerIs(person).stream().map(o -> o.getUuid()).collect(Collectors.toList());
			List<OrgUnit> orgUnits = orgUnitService.getByUuid(dto.getConstraintOrgUnitUuids());

			for (OrgUnit orgUnit : orgUnits) {
				if (managedOrgUnitUuids.contains(orgUnit.getUuid())) {
					SubstituteAssignmentOrgUnitMapping cm = new SubstituteAssignmentOrgUnitMapping();
					cm.setSubstituteAssignment(substituteAssignment);
					cm.setOrgUnit(orgUnit);
					constraintMappings.add(cm);
				}
			}
		}
		
		substituteAssignment.setConstraintMappings(constraintMappings);
		substituteAssignment.setPerson(person);
		substituteAssignment.setSubstitute(substitute);

		substituteAssignmentService.save(substituteAssignment);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@PostMapping(value = "/api/substitutes/assignments/{assignmentId}/edit")
	public ResponseEntity<?> editSubstituteAssignment(@PathVariable long assignmentId, @RequestBody List<String> constraintOUUuids) {
		SubstituteAssignment assignment = substituteAssignmentService.getById(assignmentId);
		if (assignment == null) {
			return new ResponseEntity<>("SubstituteAssignment with id " + assignmentId + " was not found", HttpStatus.NOT_FOUND);
		}
		
		List<String> managedOrgUnitUuids = orgUnitService.getAllWhereManagerIs(assignment.getPerson()).stream().map(o -> o.getUuid()).collect(Collectors.toList());
		List<String> existingOUUuids = assignment.getConstraintMappings().stream().map(c -> c.getOrgUnit().getUuid()).collect(Collectors.toList());
		if (assignment.getContext().isSupportsConstraints() && constraintOUUuids != null) {
			List<OrgUnit> orgUnits = orgUnitService.getByUuid(constraintOUUuids);

			for (OrgUnit orgUnit : orgUnits) {
				if (!existingOUUuids.contains(orgUnit.getUuid()) && managedOrgUnitUuids.contains(orgUnit.getUuid())) {
					SubstituteAssignmentOrgUnitMapping cm = new SubstituteAssignmentOrgUnitMapping();
					cm.setSubstituteAssignment(assignment);
					cm.setOrgUnit(orgUnit);
					assignment.getConstraintMappings().add(cm);
				}
			}
		}
		
		List<SubstituteAssignmentOrgUnitMapping> toDelete = assignment.getConstraintMappings().stream().filter(c -> !constraintOUUuids.contains(c.getOrgUnit().getUuid())).collect(Collectors.toList());
		assignment.getConstraintMappings().removeAll(toDelete);
		
		substituteAssignmentService.save(assignment);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
	
	@PostMapping(value = "/api/substitutes/{assignmentId}/delete")
	public ResponseEntity<?> deleteSubstituteAssignemnt(@PathVariable long assignmentId) {
		SubstituteAssignment assignment = substituteAssignmentService.getById(assignmentId);
		if (assignment == null) {
			return new ResponseEntity<>("SubstituteAssignment with id " + assignmentId + " was not found", HttpStatus.NOT_FOUND);
		}
		
		substituteAssignmentService.delete(assignment);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
