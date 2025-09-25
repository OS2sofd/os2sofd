package dk.digitalidentity.sofd.controller.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

import dk.digitalidentity.sofd.controller.api.dto.ManagerSubstitutePersonDTO;
import dk.digitalidentity.sofd.controller.api.dto.OUConstraintDTO;
import dk.digitalidentity.sofd.controller.api.dto.SubstituteAssignmentCreateDTO;
import dk.digitalidentity.sofd.controller.api.dto.SubstituteAssignmentDTO;
import dk.digitalidentity.sofd.controller.api.dto.SubstituteContextDTO;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SubstituteAssignment;
import dk.digitalidentity.sofd.dao.model.SubstituteAssignmentOrgUnitMapping;
import dk.digitalidentity.sofd.dao.model.SubstituteContext;
import dk.digitalidentity.sofd.dao.model.SubstituteOrgUnitAssignment;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.EventType;
import dk.digitalidentity.sofd.log.AuditLogger;
import dk.digitalidentity.sofd.security.RequireDaoWriteAccess;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SubstituteAssignmentService;
import dk.digitalidentity.sofd.service.SubstituteContextService;
import dk.digitalidentity.sofd.service.SubstituteOrgUnitAssignmentService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import dk.digitalidentity.sofd.telephony.controller.rest.dto.AutoCompleteResult;
import lombok.extern.slf4j.Slf4j;

@RequireReadAccess
@RestController
@Slf4j
public class SubstituteApiController {

	@Autowired
	private PersonService personService;
	
	@Autowired
	private SubstituteContextService substituteContextService;
	
	@Autowired
	private SubstituteAssignmentService substituteAssignmentService;
	
	@Autowired
	private OrgUnitService orgUnitService;

	@Autowired
	private SubstituteOrgUnitAssignmentService substituteOrgUnitAssignmentService;

	@Autowired
	private AuditLogger auditLogger;

	@GetMapping("/api/substitutes/assignments")
	public ResponseEntity<?> getAllSubstituteAssignments() {
		List<SubstituteAssignmentDTO> result = new ArrayList<>();
		
		List<SubstituteAssignment> assignments = substituteAssignmentService.findAll().stream()
				.filter(a -> a.getContext().getIdentifier().equals("os2rollekatalog") || a.getContext().getIdentifier().equals("GLOBAL"))
				.collect(Collectors.toList());
		
		for (SubstituteAssignment assignment : assignments) {
			Person manager = assignment.getPerson();
			if (manager == null) {
				continue;
			}
			
			// denne filtrerer ned til 46 fra 52 (så 6 minus)
			User managerPrimeUser = personGetPrimeUser(manager);
			if (managerPrimeUser == null) {
				continue;
			}
			
			Person substitute = assignment.getSubstitute();
			if (substitute == null) {
				continue;
			}

			// denne filtrerer ned til 49 fra 52, så 3 minutes
			User substitutPrimeUser = personGetPrimeUser(substitute);
			if (substitutPrimeUser == null) {
				continue;
			}
			
			// afhængig af overlap, kan vi have op til 9 filtreret væk her, men det er stadig 43 i output (og vi får 23)
			
			SubstituteAssignmentDTO dto = new SubstituteAssignmentDTO();

			if (assignment.getContext().getIdentifier().equals("GLOBAL")) {
				List<OrgUnit> managerOrgUnits = orgUnitService.getAllWhereManagerIs(manager);
				
				dto.setConstraintOrgUnits(managerOrgUnits.stream()
						.filter(o -> !o.isDeleted())
						.map(ou -> OUConstraintDTO.builder().name(ou.getName()).uuid(ou.getUuid()).build())
						.collect(Collectors.toList()));
			}
			else {
				if (assignment.getConstraintMappings().size() > 0) {
					dto.setConstraintOrgUnits(assignment.getConstraintMappings().stream()
							.filter(m -> !m.getOrgUnit().isDeleted())
							.map(c -> OUConstraintDTO.builder().name(c.getOrgUnit().getName()).uuid(c.getOrgUnit().getUuid()).build())
							.collect(Collectors.toList()));					
				}
				else {
					List<OrgUnit> managerOrgUnits = orgUnitService.getAllWhereManagerIs(manager);

					dto.setConstraintOrgUnits(managerOrgUnits.stream()
							.filter(o -> !o.isDeleted())
							.map(ou -> OUConstraintDTO.builder().name(ou.getName()).uuid(ou.getUuid()).build())
							.collect(Collectors.toList()));
				}
			}
			
			// need at least one OU for this to make sense
			if (dto.getConstraintOrgUnits().size() == 0) {
				continue;
			}

			dto.setId(assignment.getId());
			dto.setSubstituteContextId(assignment.getContext().getId());
			dto.setSubstituteContextName(assignment.getContext().getName());

			ManagerSubstitutePersonDTO managerDTO = new ManagerSubstitutePersonDTO();
			managerDTO.setName(PersonService.getName(manager));

			managerDTO.setUserId(managerPrimeUser.getUserId());
			managerDTO.setUuid(managerPrimeUser.getActiveDirectoryDetails().getKombitUuid());
			
			dto.setManager(managerDTO);

			ManagerSubstitutePersonDTO substituteDTO = new ManagerSubstitutePersonDTO();
			substituteDTO.setName(PersonService.getName(substitute));
			substituteDTO.setUuid(substitute.getUuid());
			substituteDTO.setUserId(substitutPrimeUser.getUserId());
			substituteDTO.setUuid(substitutPrimeUser.getActiveDirectoryDetails().getKombitUuid());

			dto.setSubstitute(substituteDTO);

			result.add(dto);
		}
		result.addAll(getOrgunitSubstituteAssignments());
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	private List<SubstituteAssignmentDTO> getOrgunitSubstituteAssignments() {
		var result = new ArrayList<SubstituteAssignmentDTO>();
		var orgUnitAssigments = substituteOrgUnitAssignmentService.getAll().stream()
				.filter(a -> a.getContext().getIdentifier().equals("os2rollekatalog") || a.getContext().getIdentifier().equals("GLOBAL"))
				.toList();

		for( var orgUnitAssigment : orgUnitAssigments ) {
			var manager = orgUnitAssigment.getOrgUnit().getManager();
			if( manager == null ) {
				continue;
			}
			var managerPrimeUser = personGetPrimeUser(orgUnitAssigment.getOrgUnit().getManager().getManager());
			if (managerPrimeUser == null) {
				continue;
			}
			var substitutPrimeUser = personGetPrimeUser(orgUnitAssigment.getSubstitute());
			if (substitutPrimeUser == null) {
				continue;
			}

			var substituteAssignmentDTO = new SubstituteAssignmentDTO();

			var substituteDTO = new ManagerSubstitutePersonDTO();
			substituteDTO.setName(PersonService.getName(orgUnitAssigment.getSubstitute()));
			substituteDTO.setUserId(substitutPrimeUser.getUserId());
			substituteDTO.setUuid(substitutPrimeUser.getActiveDirectoryDetails().getKombitUuid());
			substituteAssignmentDTO.setSubstitute(substituteDTO);

			var managerDTO = new ManagerSubstitutePersonDTO();
			managerDTO.setName(PersonService.getName(manager.getManager()));
			managerDTO.setUserId(managerPrimeUser.getUserId());
			managerDTO.setUuid(managerPrimeUser.getActiveDirectoryDetails().getKombitUuid());
			substituteAssignmentDTO.setManager(managerDTO);

			substituteAssignmentDTO.setSubstituteContextId(orgUnitAssigment.getContext().getId());
			substituteAssignmentDTO.setSubstituteContextName(orgUnitAssigment.getContext().getName());
			substituteAssignmentDTO.setOrgUnitAssignment(true);


			var constraintOrgUnits = new ArrayList<OUConstraintDTO>();
			if( !orgUnitAssigment.getContext().isInheritOrgUnitAssignments()) {
				constraintOrgUnits.add( OUConstraintDTO.builder().name(orgUnitAssigment.getOrgUnit().getName()).uuid(orgUnitAssigment.getOrgUnit().getUuid()).build());
			}
			else {
				var subOrgUnits = orgUnitService.getAllWithChildren(List.of(orgUnitAssigment.getOrgUnit().getUuid())).stream().filter(o -> !o.isDeleted()).toList();
				for( var orgUnit : subOrgUnits ) {
					constraintOrgUnits.add( OUConstraintDTO.builder().name(orgUnit.getName()).uuid(orgUnit.getUuid()).build());
				}
			}
			substituteAssignmentDTO.setConstraintOrgUnits(constraintOrgUnits);
			result.add(substituteAssignmentDTO);
		}

		return result;
	}
	
	@GetMapping("/api/substitutes/assignments/{uuid}")
	public ResponseEntity<?> getSubstituteAssignments(@PathVariable String uuid) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>("Person with uuid " + uuid + " was not found", HttpStatus.NOT_FOUND);
		}
		
		List<SubstituteAssignmentDTO> result = new ArrayList<>();
		for (SubstituteAssignment assignment : person.getSubstitutes()) {
			SubstituteAssignmentDTO dto = new SubstituteAssignmentDTO();
			dto.setConstraintOrgUnits(assignment.getConstraintMappings().stream().map(c -> OUConstraintDTO.builder().name(c.getOrgUnit().getName()).uuid(c.getOrgUnit().getUuid()).assignmentId(assignment.getId()).build()).collect(Collectors.toList()));
			dto.setId(assignment.getId());
			dto.setSubstituteContextId(assignment.getContext().getId());
			dto.setSubstituteContextName(assignment.getContext().getName());
			
			ManagerSubstitutePersonDTO substitute = new ManagerSubstitutePersonDTO();
			substitute.setName(PersonService.getName(assignment.getSubstitute()));
			substitute.setUuid(assignment.getSubstitute().getUuid());
			dto.setSubstitute(substitute);
			
			result.add(dto);
		}

		// handle orgUnit substitutes
		Map<String, List<SubstituteOrgUnitAssignment>> assignmentMap = new HashMap<>();
		List<OrgUnit> managedOrgUnits = new ArrayList<>();
		List<String> addedUuids = new ArrayList<>();
		for (OrgUnit orgUnit : orgUnitService.getAllWhereManagerIs(person)) {
			addInheritedManagedOrgUnitsRecursive(managedOrgUnits, addedUuids, orgUnit);
		}

		for (OrgUnit ou : managedOrgUnits) {
			for (SubstituteOrgUnitAssignment assignment : ou.getSubstitutes()) {
				String key = assignment.getSubstitute().getUuid() + ";" + assignment.getContext().getIdentifier();
				if (!assignmentMap.containsKey(key)) {
					assignmentMap.put(key, new ArrayList<>());
				}
				assignmentMap.get(key).add(assignment);
			}
		}

		for (List<SubstituteOrgUnitAssignment> assignmentList :assignmentMap.values()) {
			SubstituteAssignmentDTO dto = new SubstituteAssignmentDTO();
			dto.setConstraintOrgUnits(assignmentList.stream().map(a -> OUConstraintDTO.builder().name(a.getOrgUnit().getName()).uuid(a.getOrgUnit().getUuid()).assignmentId(a.getId()).build()).collect(Collectors.toList()));
			dto.setSubstituteContextId(assignmentList.get(0).getContext().getId());
			dto.setSubstituteContextName(assignmentList.get(0).getContext().getName());
			dto.setOrgUnitAssignment(true);

			ManagerSubstitutePersonDTO substitute = new ManagerSubstitutePersonDTO();
			substitute.setName(PersonService.getName(assignmentList.get(0).getSubstitute()));
			substitute.setUuid(assignmentList.get(0).getSubstitute().getUuid());
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
			dto.setAssignableToOrgunit(context.isAssignableToOrgUnit());
			dto.setInherit(context.isInheritOrgUnitAssignments());
			
			result.add(dto);
		}
		
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
	
	@GetMapping(value = "/api/substitutes/search/person/{uuid}")
	public ResponseEntity<?> searchPerson(@PathVariable("uuid") String uuid, @RequestParam("query") String term, @RequestParam(name = "ous", required = false) String ous) {
		AutoCompleteResult result = personService.substituteSearchPerson(term, uuid, ous);
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
	
	@GetMapping("/api/substitutes/managedorgunits/{uuid}")
	public ResponseEntity<?> getManagedOUs(@PathVariable String uuid) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>("Person with uuid " + uuid + " was not found", HttpStatus.NOT_FOUND);
		}

		// include the ous, where the person is indirect manager (even if that ou has another manager)
		List<String> addedUuids = new ArrayList<>();
		List<OUConstraintDTO> managedOrgUnits = new ArrayList<>();
		for (OrgUnit orgUnit : orgUnitService.getAllWhereManagerIs(person)) {
			addManagedOrgUnitsRecursive(addedUuids, managedOrgUnits, orgUnit);
		}

		return new ResponseEntity<>(managedOrgUnits, HttpStatus.OK);
	}

	@RequireDaoWriteAccess
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
			List<OrgUnit> orgUnits = orgUnitService.getByUuid(dto.getConstraintOrgUnitUuids());

			// include the ous, where the person is indirect manager (even if that ou has another manager)
			List<String> managedOrgUnitUuids = new ArrayList<>();
			for (OrgUnit orgUnit : orgUnitService.getAllWhereManagerIs(person)) {
				addManagedOrgUnitsRecursive(managedOrgUnitUuids, orgUnit);
			}

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

		var assignment = substituteAssignmentService.save(substituteAssignment);
		var message = "Stedfortræder (" + assignment.getContext().getName() + ") '" + assignment.getSubstitute().getEntityName() + "' for leder '" + assignment.getPerson().getEntityName() + "' oprettet via API";
		log.info(message);
		auditLogger.log(String.valueOf(assignment.getId()), EntityType.SUBSTITUTE_ASSIGNMENT, EventType.SAVE,assignment.getSubstitute().getEntityName(), message, dto.getUserId());

		return new ResponseEntity<>(HttpStatus.OK);
	}

	record AddOrgUnitSubstituteDTO(long substituteContextId, String substitutePersonUuid, String orgUnitUuid, String userId) {}
	
	@RequireDaoWriteAccess
	@PostMapping(value = "/api/substitutes/orgunit/assignments/create")
	public ResponseEntity<?> createSubstituteAssignment(@RequestBody AddOrgUnitSubstituteDTO dto) {
		OrgUnit orgUnit = orgUnitService.getByUuid(dto.orgUnitUuid());
		if (orgUnit == null) {
			return new ResponseEntity<>("OrgUnit with uuid " + dto.orgUnitUuid() + " was not found", HttpStatus.NOT_FOUND);
		}

		Person substitute = personService.getByUuid(dto.substitutePersonUuid());
		if (substitute == null) {
			return new ResponseEntity<>("Person with uuid " + dto.substitutePersonUuid() + " was not found", HttpStatus.NOT_FOUND);
		}

		SubstituteContext context = substituteContextService.getById(dto.substituteContextId());
		if (context == null) {
			return new ResponseEntity<>("SubstituteContext with id " + dto.substituteContextId() + " was not found", HttpStatus.NOT_FOUND);
		}

		SubstituteOrgUnitAssignment substituteAssignment = new SubstituteOrgUnitAssignment();
		substituteAssignment.setContext(context);
		substituteAssignment.setOrgUnit(orgUnit);
		substituteAssignment.setSubstitute(substitute);


		var assignment = substituteOrgUnitAssignmentService.save(substituteAssignment);
		var message = "Stedfortræder (" + assignment.getContext().getName() + ") '" + assignment.getSubstitute().getEntityName() + "' for enhed '" + assignment.getOrgUnit().getEntityName() + "' oprettet via API";
		log.info(message);
		auditLogger.log(String.valueOf(assignment.getId()), EntityType.SUBSTITUTE_ORGUNIT_ASSIGNMENT, EventType.SAVE,assignment.getSubstitute().getEntityName(), message, dto.userId());

		return new ResponseEntity<>(HttpStatus.OK);
	}

	record EditSubstituteDTO(List<String> constraintOUUuids, String userId) {}
	
	@RequireDaoWriteAccess
	@PostMapping(value = "/api/substitutes/assignments/{assignmentId}/edit")
	public ResponseEntity<?> editSubstituteAssignment(@PathVariable long assignmentId, @RequestBody EditSubstituteDTO dto ) {
		SubstituteAssignment assignment = substituteAssignmentService.getById(assignmentId);
		if (assignment == null) {
			return new ResponseEntity<>("SubstituteAssignment with id " + assignmentId + " was not found", HttpStatus.NOT_FOUND);
		}

		// include the ous, where the person is indirect manager (even if that ou has another manager)
		List<String> managedOrgUnitUuids = new ArrayList<>();
		for (OrgUnit orgUnit : orgUnitService.getAllWhereManagerIs(assignment.getPerson())) {
			addManagedOrgUnitsRecursive(managedOrgUnitUuids, orgUnit);
		}

		List<String> existingOUUuids = assignment.getConstraintMappings().stream().map(c -> c.getOrgUnit().getUuid()).collect(Collectors.toList());
		if (assignment.getContext().isSupportsConstraints() && dto.constraintOUUuids() != null) {
			List<OrgUnit> orgUnits = orgUnitService.getByUuid(dto.constraintOUUuids());

			for (OrgUnit orgUnit : orgUnits) {
				if (!existingOUUuids.contains(orgUnit.getUuid()) && managedOrgUnitUuids.contains(orgUnit.getUuid())) {
					SubstituteAssignmentOrgUnitMapping cm = new SubstituteAssignmentOrgUnitMapping();
					cm.setSubstituteAssignment(assignment);
					cm.setOrgUnit(orgUnit);
					assignment.getConstraintMappings().add(cm);
				}
			}
		}
		
		List<SubstituteAssignmentOrgUnitMapping> toDelete = assignment.getConstraintMappings().stream().filter(c -> !dto.constraintOUUuids().contains(c.getOrgUnit().getUuid())).collect(Collectors.toList());
		assignment.getConstraintMappings().removeAll(toDelete);
		
		assignment = substituteAssignmentService.save(assignment);
		var message = "Stedfortræder (" + assignment.getContext().getName() + ") '" + assignment.getSubstitute().getEntityName() + "' for leder '" + assignment.getPerson().getEntityName() + "' redigeret via API";
		log.info(message);
		auditLogger.log(String.valueOf(assignment.getId()), EntityType.SUBSTITUTE_ASSIGNMENT, EventType.SAVE,assignment.getSubstitute().getEntityName(), message, dto.userId());
		
		return new ResponseEntity<>(HttpStatus.OK);
	}

	record deleteSubstituteDTO(long assignmentId, String userId) {}
	
	@RequireDaoWriteAccess
	@PostMapping(value = "/api/substitutes/{assignmentId}/delete")
	public ResponseEntity<?> deleteSubstituteAssignemnt(@RequestBody deleteSubstituteDTO dto) {
		SubstituteAssignment assignment = substituteAssignmentService.getById(dto.assignmentId());
		if (assignment == null) {
			return new ResponseEntity<>("SubstituteAssignment with id " + dto.assignmentId() + " was not found", HttpStatus.NOT_FOUND);
		}

		var message = "Stedfortræder (" + assignment.getContext().getName() + ") '" + assignment.getSubstitute().getEntityName() + "' for leder '" + assignment.getPerson().getEntityName() + "' slettet via API";
		log.info(message);
		auditLogger.log(String.valueOf(assignment.getId()), EntityType.SUBSTITUTE_ASSIGNMENT, EventType.DELETE,assignment.getSubstitute().getEntityName(), message, dto.userId());
		substituteAssignmentService.delete(assignment);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}

	record deleteOrgUnitSubstituteDTO(long assignmentId, String userId) {}
	
	@RequireDaoWriteAccess
	@PostMapping(value = "/api/substitutes/orgunit/{assignmentId}/delete")
	public ResponseEntity<?> deleteSubstituteOrgUnitAssignemnt(@RequestBody deleteOrgUnitSubstituteDTO dto) {
		SubstituteOrgUnitAssignment assignment = substituteOrgUnitAssignmentService.getById(dto.assignmentId());
		if (assignment == null) {
			return new ResponseEntity<>("SubstituteOrgUnitAssignment with id " + dto.assignmentId() + " was not found", HttpStatus.NOT_FOUND);
		}

		var message = "Stedfortræder (" + assignment.getContext().getName() + ") '" + assignment.getSubstitute().getEntityName() + "' for enhed '" + assignment.getOrgUnit().getEntityName() + "' slettet via API";
		log.info(message);
		auditLogger.log(String.valueOf(assignment.getId()), EntityType.SUBSTITUTE_ORGUNIT_ASSIGNMENT, EventType.DELETE,assignment.getSubstitute().getEntityName(), message, dto.userId());
		substituteOrgUnitAssignmentService.delete(assignment);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	private void addManagedOrgUnitsRecursive(List<String> addedUuids, List<OUConstraintDTO> managedOrgUnits, OrgUnit current) {
		if (!addedUuids.contains(current.getUuid())) {
			managedOrgUnits.add(OUConstraintDTO.builder().name(current.getName()).uuid(current.getUuid()).build());
			addedUuids.add(current.getUuid());
		}

		for (OrgUnit child : current.getChildren()) {
			addManagedOrgUnitsRecursive(addedUuids, managedOrgUnits, child);
		}
	}

	private void addManagedOrgUnitsRecursive(List<String> addedUuids, OrgUnit current) {
		if (!addedUuids.contains(current.getUuid())) {
			addedUuids.add(current.getUuid());
		}

		for (OrgUnit child : current.getChildren()) {
			addManagedOrgUnitsRecursive(addedUuids, child);
		}
	}

	private void addInheritedManagedOrgUnitsRecursive(List<OrgUnit> orgUnits, List<String> addedUuids, OrgUnit current) {
		if (!addedUuids.contains(current.getUuid())) {
			addedUuids.add(current.getUuid());
			orgUnits.add(current);
		}

		for (OrgUnit child : current.getChildren()) {
			addInheritedManagedOrgUnitsRecursive(orgUnits, addedUuids, child);
		}
	}
	
	private User personGetPrimeUser(Person person) {
		return PersonService.getUsers(person).stream()
				.filter(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType()) && u.isPrime())
				.findFirst()
				.orElse(null);
	}
}
