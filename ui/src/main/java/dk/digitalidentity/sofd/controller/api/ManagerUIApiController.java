package dk.digitalidentity.sofd.controller.api;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonFormat;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.api.dto.LoginContextDTO;
import dk.digitalidentity.sofd.controller.api.dto.LoginContextResultDTO;
import dk.digitalidentity.sofd.controller.api.dto.LoginContextRole;
import dk.digitalidentity.sofd.dao.model.AccountOrder;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.PersonLeave;
import dk.digitalidentity.sofd.dao.model.SubstituteAssignment;
import dk.digitalidentity.sofd.dao.model.SubstituteOrgUnitAssignment;
import dk.digitalidentity.sofd.dao.model.enums.EventType;
import dk.digitalidentity.sofd.dao.model.enums.LeaveReason;
import dk.digitalidentity.sofd.log.AuditLogger;
import dk.digitalidentity.sofd.security.RequireDaoWriteAccess;
import dk.digitalidentity.sofd.service.AccountOrderService;
import dk.digitalidentity.sofd.service.AffiliationService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SubstituteAssignmentService;
import dk.digitalidentity.sofd.service.SubstituteOrgUnitAssignmentService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;

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

	@Autowired
	private AccountOrderService accountOrderService;

	@Autowired
	private AuditLogger auditLogger;

	@Autowired
	private MessageSource messageSource;

	private Locale locale = new Locale("da-DK");

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

	private record PausablePersonDTO(String uuid, String personName, LeaveDTO leaveDTO, List<String> positions) {}
	private record LeaveDTO(@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")  Date startDate, @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") Date stopDate, String leaveReasonValue, String leaveReasonMessage, String reasonText, boolean disableAccountOrders, boolean expireAccounts) {}
	@GetMapping("/api/manager/{uuid}/pausablepeople")
	public ResponseEntity<?> getPausablePeople(@PathVariable String uuid) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		List<PausablePersonDTO> result = new ArrayList<>();
		List<Person> pausablePeople = getPeopleForManager(person);
		if (pausablePeople != null) {
			for (Person pausablePerson : pausablePeople) {
				LeaveDTO leaveDTO = null;
				if (pausablePerson.getLeave() != null) {
					leaveDTO = new LeaveDTO(pausablePerson.getLeave().getStartDate(), pausablePerson.getLeave().getStopDate(), pausablePerson.getLeave().getReason().toString(), messageSource.getMessage(pausablePerson.getLeave().getReason().getMessage(), null, locale), pausablePerson.getLeave().getReasonText(), pausablePerson.getLeave().isDisableAccountOrders(), pausablePerson.getLeave().isExpireAccounts());
				}

				List<String> positions = new ArrayList<>();
				List<Affiliation> activeAffiliations = AffiliationService.onlyActiveAffiliations(pausablePerson.getAffiliations());
				for (Affiliation activeAffiliation : activeAffiliations) {
					positions.add(activeAffiliation.getPositionName() + " i " + activeAffiliation.getCalculatedOrgUnit().getName());
				}

				PausablePersonDTO pausablePersonDTO = new PausablePersonDTO(pausablePerson.getUuid(), PersonService.getName(pausablePerson), leaveDTO, positions);
				result.add(pausablePersonDTO);
			}
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

	public record EditLeaveDTO(String uuid, boolean paused, @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy") Date startDate, @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy") Date stopDate, LeaveReason reason, String reasonText, boolean disableAccountOrders, boolean expireAccounts) {}
	@PostMapping("/api/manager/{uuid}/leave/edit")
	public ResponseEntity<?> editLeave(@PathVariable String uuid, @RequestBody EditLeaveDTO dto) {
		Person manager = personService.getByUuid(uuid);
		if (manager == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		List<Person> result = getPeopleForManager(manager);
		if (result == null) {
			return new ResponseEntity<>("The manager is not a mananger", HttpStatus.BAD_REQUEST);
		}

		Person match = result.stream().filter(p -> p.getUuid().equals(dto.uuid())).findAny().orElse(null);
		if (match == null) {
			return new ResponseEntity<>("Failed to find editable manager with id " + dto.uuid(), HttpStatus.BAD_REQUEST);
		}

		Date startDate = dto.startDate();
		if (startDate == null) {
			startDate = new Date();
		}

		Date stopDate = dto.stopDate();
		if (stopDate != null && stopDate.before(startDate)) {
			return new ResponseEntity<>("StopDate can't be before startDate " + dto.uuid(), HttpStatus.BAD_REQUEST);
		}

		List<AccountOrder> newOrders = new ArrayList<AccountOrder>();

		if (dto.paused()) {
			if (match.getLeave() == null) {
				match.setLeave(new PersonLeave());

				// can only be modified on creation of leave
				match.getLeave().setExpireAccounts(dto.expireAccounts());
				match.getLeave().setStartDate(dto.startDate());

				boolean disableAccountOrders = dto.disableAccountOrders();
				if (dto.expireAccounts()) {
					List<AccountOrder> orders = personService.generateExpireOrders(match, startDate, startDate);
					newOrders.addAll(orders);

					// the UI enforces this, but lets make sure
					disableAccountOrders = true;
				}

				match.getLeave().setDisableAccountOrders(dto.disableAccountOrders());

				// the leaveForm can order the setting of this flag (it can be removed from the usual dialogue though
				// so this is just an easy way to do two things in one go)
				if (disableAccountOrders) {
					match.setDisableAccountOrders(true);
				}
			}

			// these are modifiable on existing leave data
			match.getLeave().setStopDate(stopDate);
			match.getLeave().setReason(dto.reason());
			match.getLeave().setReasonText(dto.reasonText());
		}
		else {
			List<AccountOrder> orders = personService.removeLeave(match);
			newOrders.addAll(orders);
		}

		// we have to loop (there won't be many) as the saveAll method has some nasty side-effects
		for (AccountOrder order : newOrders) {
			accountOrderService.save(order);
		}

		personService.save(match);

		auditLogger.log(match, EventType.PERSON_CHANGED, (match.getLeave() != null ? "Pausemarkering sat på manager" : "Pausemarkering fjernet fra manager"));

		return new ResponseEntity<>(HttpStatus.OK);
	}

	private List<Affiliation> getEditableAffiliationsForManager(Person manager) {
		List<OrgUnit> managerOus = orgUnitService.getAllWhereManagerIs(manager);
		List<Affiliation> result = new ArrayList<>();
		for (OrgUnit ou : managerOus) {
			List<Affiliation> affiliationsForOu = affiliationService.findByOrgUnit(ou);
			result.addAll(affiliationsForOu.stream().filter(a -> a.getMaster().equals("SOFD")).collect(Collectors.toList()));
		}

		return result;
	}

	private List<Person> getPeopleForManager(Person manager) {
		List<OrgUnit> managerOus = orgUnitService.getAllWhereManagerIs(manager);
		if (managerOus.isEmpty()) {
			return null;
		}

		List<String> addedUuids = new ArrayList<>();
		List<Person> result = new ArrayList<>();
		for (OrgUnit ou : managerOus) {
			List<Affiliation> affiliationsForOu = affiliationService.findByCalculatedOrgUnitAndActive(ou);
			for (Affiliation affiliation : affiliationsForOu) {
				if (!addedUuids.contains(affiliation.getPerson().getUuid())) {
					result.add(affiliation.getPerson());
					addedUuids.add(affiliation.getPerson().getUuid());
				}
			}
		}

		return result;
	}
}
