package dk.digitalidentity.sofd.controller.api;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.controller.api.dto.LoginContextRole;
import dk.digitalidentity.sofd.controller.api.dto.LoginContextDTO;
import dk.digitalidentity.sofd.controller.api.dto.LoginContextResultDTO;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SubstituteAssignment;
import dk.digitalidentity.sofd.security.RequireDaoWriteAccess;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SubstituteAssignmentService;

@RequireDaoWriteAccess
@RestController
public class ManagerUIApiController {
	
	@Autowired
	private PersonService personService;
	
	@Autowired
	private SubstituteAssignmentService substituteAssignmentService;
	
	@Autowired
	private OrgUnitService orgUnitService;

	@GetMapping("/api/manager/{uuid}/logincontexts")
	public ResponseEntity<?> getLoginContexts(@PathVariable String uuid) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		LoginContextResultDTO result = new LoginContextResultDTO();
		result.setName(PersonService.getName(person));
		
		List<LoginContextDTO> contexts = new ArrayList<>();
		for (SubstituteAssignment assignment : substituteAssignmentService.findBySubstitute(person)) {
			LoginContextDTO dto = new LoginContextDTO();
			dto.setName(PersonService.getName(assignment.getPerson()));
			dto.setRole(LoginContextRole.SUBSTITUTE);
			dto.setUuid(assignment.getPerson().getUuid());
			
			if (assignment.getConstraintMappings() != null && !assignment.getConstraintMappings().isEmpty()) {
				dto.setOuUuids(assignment.getConstraintMappings().stream().map(c -> c.getOrgUnit().getUuid()).collect(Collectors.joining(",")));
			} else {
				dto.setOuUuids(orgUnitService.getAllWhereManagerIs(assignment.getPerson()).stream().filter(o -> !o.getManager().isInherited()).map(o -> o.getUuid()).collect(Collectors.joining(",")));
			}
			
			contexts.add(dto);
		}
		
		String directManagerOus = orgUnitService.getAllWhereManagerIs(person).stream().filter(o -> !o.getManager().isInherited()).map(o -> o.getUuid()).collect(Collectors.joining(","));
		if (!directManagerOus.isEmpty()) {
			LoginContextDTO dto = new LoginContextDTO();
			dto.setName(PersonService.getName(person));
			dto.setRole(LoginContextRole.MANAGER);
			dto.setUuid(person.getUuid());
			dto.setOuUuids(directManagerOus);
			
			contexts.add(dto);
		}
		
		result.setContexts(contexts);
		
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
}
