package dk.digitalidentity.sofd.controller.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.*;
import dk.digitalidentity.sofd.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonFormat;

import dk.digitalidentity.sofd.controller.api.dto.LoginContextDTO;
import dk.digitalidentity.sofd.controller.api.dto.LoginContextResultDTO;
import dk.digitalidentity.sofd.controller.api.dto.LoginContextRole;
import dk.digitalidentity.sofd.security.RequireDaoWriteAccess;

@RequireDaoWriteAccess
@RestController
public class ManagerUIApiController {
	
	@Autowired
	private PersonService personService;
	
	@Autowired
	private SubstituteAssignmentService substituteAssignmentService;
	
	@Autowired
	private OrgUnitService orgUnitService;

	@Autowired
	private SubstituteOrgUnitAssignmentService substituteOrgUnitAssignmentService;

	@Autowired
	private AffiliationService affiliationService;

	@Autowired
	private SofdConfiguration configuration;

	@GetMapping("/api/manager/{uuid}/logincontexts")
	public ResponseEntity<?> getLoginContexts(@PathVariable String uuid) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		LoginContextResultDTO result = new LoginContextResultDTO();
		result.setName(PersonService.getName(person));
		result.setUsername(person.getPrimeADAccount());
		result.setEmail(PersonService.getEmail(person));
		
		List<LoginContextDTO> contexts = new ArrayList<>();
		for (SubstituteAssignment assignment : substituteAssignmentService.findBySubstitute(person)) {
			if (Objects.equals("GLOBAL", assignment.getContext().getIdentifier()) || Objects.equals("SOFD", assignment.getContext().getIdentifier()) || Objects.equals(configuration.getModules().getManagerUI().getManagerUISubstituteContextIdentifier(), assignment.getContext().getIdentifier())) {
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

		}

		for (SubstituteOrgUnitAssignment assignment : substituteOrgUnitAssignmentService.findBySubstitute(person)) {
			if (Objects.equals("GLOBAL", assignment.getContext().getIdentifier()) || Objects.equals("SOFD", assignment.getContext().getIdentifier()) || Objects.equals(configuration.getModules().getManagerUI().getManagerUISubstituteContextIdentifier(), assignment.getContext().getIdentifier())) {
				LoginContextDTO match = contexts.stream().filter(c -> c.getRole().equals(LoginContextRole.SUBSTITUTE) && c.getUuid().equals(assignment.getOrgUnit().getManager().getManager().getUuid())).findAny().orElse(null);
				if (match == null) {
					LoginContextDTO dto = new LoginContextDTO();
					dto.setName(PersonService.getName(assignment.getOrgUnit().getManager().getManager()));
					dto.setRole(LoginContextRole.SUBSTITUTE);
					dto.setUuid(assignment.getOrgUnit().getManager().getManager().getUuid());
					dto.setOuUuids(assignment.getOrgUnit().getUuid());

					contexts.add(dto);
				}
				else {
					if (!match.getOuUuids().contains(assignment.getOrgUnit().getUuid())) {
						match.setOuUuids(match.getOuUuids() + "," + assignment.getOrgUnit().getUuid());
					}
				}

				if (assignment.getContext().isInheritOrgUnitAssignments()) {
					for (OrgUnit child : assignment.getOrgUnit().getChildren()) {
						handleContextRecursive(child, contexts);
					}
				}
			}
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

	private void handleContextRecursive(OrgUnit currentOU, List<LoginContextDTO> contexts) {
		if( currentOU.getManager() != null)
		{
			LoginContextDTO match = contexts.stream().filter(c -> c.getRole().equals(LoginContextRole.SUBSTITUTE) && c.getUuid().equals(currentOU.getManager().getManager().getUuid())).findAny().orElse(null);
			if (match == null) {
				LoginContextDTO dto = new LoginContextDTO();
				dto.setName(PersonService.getName(currentOU.getManager().getManager()));
				dto.setRole(LoginContextRole.SUBSTITUTE);
				dto.setUuid(currentOU.getManager().getManager().getUuid());
				dto.setOuUuids(currentOU.getUuid());

				contexts.add(dto);
			} else {
				if (!match.getOuUuids().contains(currentOU.getUuid())) {
					match.setOuUuids(match.getOuUuids() + "," + currentOU.getUuid());
				}
			}
		}

		for (OrgUnit child : currentOU.getChildren()) {
			handleContextRecursive(child, contexts);
		}
	}

	private record AffiliationDTO(long id, String personName, String positionName, String positionDisplayName, String orgUnitName, boolean active, @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")  Date startDate, @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") Date stopDate, String internalReference, String adUserInfo) {}
	@GetMapping("/api/manager/{uuid}/affiliations")
	public ResponseEntity<?> getEditableAffiliations(@PathVariable String uuid) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		List<Affiliation> editableAffiliations = getEditableAffiliationsForManager(person);
		if (editableAffiliations == null) {
			return new ResponseEntity<>("The person is not a mananger", HttpStatus.BAD_REQUEST);
		}
		var result = new ArrayList<AffiliationDTO>();
		for( var a : editableAffiliations )
		{
			var primeADUser = PersonService.getUsers(a.getPerson()).stream().filter(u -> Objects.equals(u.getUserType(), SupportedUserTypeService.getActiveDirectoryUserType()) && u.isPrime()).findFirst().orElse(null);
			var adUserInfo = "";
			if( primeADUser != null )
			{
				adUserInfo = primeADUser.getUserId();
				if( primeADUser.isDisabled() )
				{
					adUserInfo += " (deaktiveret)";
				}
			}
			var affiliationDto = new AffiliationDTO(a.getId(), PersonService.getName(a.getPerson()), a.getPositionName(), a.getPositionDisplayName(), a.getCalculatedOrgUnit().getName(), AffiliationService.activeAndNotStopped(a), a.getStartDate(), a.getStopDate(), a.getInternalReference(),adUserInfo);
			result.add(affiliationDto);
		}

		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	private record EditAffiliationDTO(long id, String position, String positionDisplayName, Date startDate, Date stopDate, String internalReference) {}
	@PostMapping("/api/manager/{uuid}/affiliations/edit")
	public ResponseEntity<?> editAffiliation(@PathVariable String uuid, @RequestBody EditAffiliationDTO dto) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		List<Affiliation> result = getEditableAffiliationsForManager(person);
		if (result == null) {
			return new ResponseEntity<>("The person is not a mananger", HttpStatus.BAD_REQUEST);
		}

		Affiliation match = result.stream().filter(a -> a.getId() == dto.id()).findAny().orElse(null);
		if (match == null) {
			return new ResponseEntity<>("Failed to find editable affiliation with id " + dto.id(), HttpStatus.BAD_REQUEST);
		}

		boolean changes = false;
		if (!Objects.equals(match.getPositionName(), dto.position())) {
			match.setPositionName((StringUtils.hasLength(dto.position())) ? dto.position().trim() : "Ukendt");
			changes = true;
		}
		
		if (!Objects.equals(match.getPositionDisplayName(), dto.positionDisplayName())) {
			match.setPositionDisplayName(dto.positionDisplayName());
			changes = true;
		}

		if (!Objects.equals(match.getStopDate(), dto.stopDate())) {
			match.setStopDate(dto.stopDate());
			changes = true;
		}
		
		if (!Objects.equals(match.getInternalReference(), dto.internalReference())) {
			match.setInternalReference(dto.internalReference());
			changes = true;
		}

		if (changes) {
			affiliationService.save(match);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	private List<Affiliation> getEditableAffiliationsForManager(Person manager) {
		List<OrgUnit> managerOus = orgUnitService.getAllWhereManagerIs(manager);
		if (managerOus.isEmpty()) {
			return null;
		}

		List<Affiliation> result = new ArrayList<>();
		for (OrgUnit ou : managerOus) {
			List<Affiliation> affiliationsForOu = affiliationService.findByOrgUnit(ou);
			result.addAll(affiliationsForOu.stream().filter(a -> a.getMaster().equals("SOFD")).collect(Collectors.toList()));
		}

		return result;
	}
}
