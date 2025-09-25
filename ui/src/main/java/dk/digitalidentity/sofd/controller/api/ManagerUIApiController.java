package dk.digitalidentity.sofd.controller.api;

import static dk.digitalidentity.sofd.util.DateConverter.toLocalDate;
import static dk.digitalidentity.sofd.util.NullChecker.getValue;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonFormat;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.api.dto.AccountOrderDTO;
import dk.digitalidentity.sofd.controller.api.dto.LoginContextDTO;
import dk.digitalidentity.sofd.controller.api.dto.LoginContextResultDTO;
import dk.digitalidentity.sofd.controller.api.dto.LoginContextRole;
import dk.digitalidentity.sofd.dao.model.AccountOrder;
import dk.digitalidentity.sofd.dao.model.AccountOrderApproved;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.PersonLeave;
import dk.digitalidentity.sofd.dao.model.SubstituteAssignment;
import dk.digitalidentity.sofd.dao.model.SubstituteOrgUnitAssignment;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderStatus;
import dk.digitalidentity.sofd.dao.model.enums.EventType;
import dk.digitalidentity.sofd.dao.model.enums.LeaveReason;
import dk.digitalidentity.sofd.dao.model.enums.OrgUnitManagerSource;
import dk.digitalidentity.sofd.log.AuditLogger;
import dk.digitalidentity.sofd.security.RequireDaoWriteAccess;
import dk.digitalidentity.sofd.service.AccountOrderApprovedService;
import dk.digitalidentity.sofd.service.AccountOrderService;
import dk.digitalidentity.sofd.service.AffiliationService;
import dk.digitalidentity.sofd.service.ManagerService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SubstituteAssignmentService;
import dk.digitalidentity.sofd.service.SubstituteOrgUnitAssignmentService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import lombok.extern.slf4j.Slf4j;

@RequireDaoWriteAccess
@RestController
@Slf4j
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

	@Autowired
	private AccountOrderApprovedService accountOrderApprovedService;

	@Autowired
	private ManagerService managerService;

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
				LoginContextDTO match = contexts.stream().filter(c -> c.getRole().equals(LoginContextRole.SUBSTITUTE) && assignment.getOrgUnit().getManager() != null && assignment.getOrgUnit().getManager().getManager() != null && c.getUuid().equals(assignment.getOrgUnit().getManager().getManager().getUuid())).findAny().orElse(null);				if (match == null) {
					LoginContextDTO dto = new LoginContextDTO();
					// Null checking through the chain of calls to prevent NPE in further code
					String name = ((assignment.getOrgUnit() == null ? null : assignment.getOrgUnit()) == null ? null : assignment.getOrgUnit().getManager()) == null ? null : assignment.getOrgUnit().getManager().getManager() == null ? null : PersonService.getName(assignment.getOrgUnit().getManager().getManager());
					dto.setName(name);
					dto.setRole(LoginContextRole.SUBSTITUTE);
					dto.setUuid(name == null ? null : assignment.getOrgUnit().getManager().getManager().getUuid());
					dto.setOuUuids(name == null ? null : assignment.getOrgUnit().getUuid());

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
	private record LeaveDTO(@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate startDate, @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate stopDate, String leaveReasonValue, String leaveReasonMessage, String reasonText, boolean disableAccountOrders, boolean expireAccounts) {}
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
					var startDate = toLocalDate(() -> pausablePerson.getLeave().getStartDate());
					var stopDate = toLocalDate(() -> pausablePerson.getLeave().getStopDate());
					leaveDTO = new LeaveDTO(startDate, stopDate, pausablePerson.getLeave().getReason().toString(), messageSource.getMessage(pausablePerson.getLeave().getReason().getMessage(), null, locale), pausablePerson.getLeave().getReasonText(), pausablePerson.getLeave().isDisableAccountOrders(), pausablePerson.getLeave().isExpireAccounts());
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
	public ResponseEntity<?> editAffiliation(@PathVariable String uuid, @RequestBody EditAffiliationDTO editAffiliationDTO) {
		Person manager = personService.getByUuid(uuid);
		if (manager == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if( !isAffiliationEditableByManager(manager, editAffiliationDTO.id()) ) {
			return new ResponseEntity<>("Failed to find editable affiliation with id " + editAffiliationDTO.id(), HttpStatus.BAD_REQUEST);
		}

		Person updatePerson = personService.findbyAffiliationId(editAffiliationDTO.id);
		var updateAffiliation = updatePerson.getAffiliations().stream().filter(a -> a.getId() == editAffiliationDTO.id()).findFirst().orElse(null);

		boolean changes = false;
		if (!Objects.equals(updateAffiliation.getPositionName(), editAffiliationDTO.position())) {
			updateAffiliation.setPositionName((StringUtils.hasLength(editAffiliationDTO.position())) ? editAffiliationDTO.position().trim() : "Ukendt");
			changes = true;
		}

		if (!Objects.equals(updateAffiliation.getPositionDisplayName(), editAffiliationDTO.positionDisplayName())) {
			updateAffiliation.setPositionDisplayName(editAffiliationDTO.positionDisplayName());
			changes = true;
		}

		if (!Objects.equals(updateAffiliation.getStopDate(), editAffiliationDTO.stopDate())) {
			updateAffiliation.setStopDate(editAffiliationDTO.stopDate());
			changes = true;
		}

		if (!Objects.equals(updateAffiliation.getInternalReference(), editAffiliationDTO.internalReference())) {
			updateAffiliation.setInternalReference(editAffiliationDTO.internalReference());
			changes = true;
		}

		if (changes) {
			personService.save(updatePerson);
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
				if (dto.disableAccountOrders) {
					match.getLeave().setDisableAccountOrders(true);
				}

				// the leaveForm can order the setting of this flag (it can be removed from the usual dialogue though
				// so this is just an easy way to do two things in one go)
				if (disableAccountOrders) {
					match.setDisableAccountOrdersCreate(true);
					match.setDisableAccountOrdersDelete(true);
					match.setDisableAccountOrdersDisable(true);
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

		String message;
		if (match.getLeave() == null) {
			message = "Pausemarkering fjernet via OS2sofd lederside";
		}
		else {
			var startDato = match.getLeave().getStartDate() != null ? new SimpleDateFormat("yyyy-MM-dd").format(match.getLeave().getStartDate()) : "ingen";
			var stopDato = match.getLeave().getStopDate() != null ? new SimpleDateFormat("yyyy-MM-dd").format(match.getLeave().getStopDate()) : "ingen";
			message = "Pausemarkering oprettet/redigere via OS2sofd lederside. Startdato: " + startDato + ", slutdato: " + stopDato;
		}

		auditLogger.log(match, EventType.PERSON_CHANGED, message);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping("/api/manager/{uuid}/accountorders")
	public ResponseEntity<?> getAccountOrders(@PathVariable String uuid) {
		//checks if the manager (account performing the approval) exists
		Person manager = personService.getByUuid(uuid);
		if (manager == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		//finds all PENDNG_APPROVAL orders
		List<AccountOrder> orders = accountOrderService.findByStatusIn(Set.of(AccountOrderStatus.PENDING_APPROVAL));

		//Filters the orders, that the manager is allowed to approve and add them to the list in the ResponseEntity
		List<AccountOrderDTO> orderDTOs = new ArrayList<>();
		for (AccountOrder order : orders) {
			Person orderedPerson = personService.getByUuid(order.getPersonUuid());
			if (orderedPerson == null) {
				log.warn("Could not find person with uuid: " + order.getPersonUuid());
				continue;
			}
			//Adds to list if Person manager (from above) is the same manager of the order
			if (PersonService.getManager(order.getTriggerAffiliation(), personService.getByUuid(order.getPersonUuid()),order.getEmployeeId()) == manager) {
				orderDTOs.add(new AccountOrderDTO(order, null, orderedPerson, true));
			}
		}
		return new ResponseEntity<>(orderDTOs, HttpStatus.OK);
	}

	@PostMapping("/api/manager/{uuid}/accountorders/approve/{id}")
	public ResponseEntity<?> approveAccountOrders(@PathVariable String uuid, @PathVariable long id) {
		//checks if the manager (account performing the approval) exists
		Person manager = personService.getByUuid(uuid);
		if (manager == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		//checks if order exists
		AccountOrder order = accountOrderService.findById(id);
		if (order == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		//changes accountOrderStatus and saves order
		order.setStatus(AccountOrderStatus.PENDING);
		accountOrderService.save(order);

		//Logging
		AccountOrderApproved approval = new AccountOrderApproved();
		approval.setApprovedTts(LocalDateTime.now());
		approval.setApproverName(PersonService.getName(manager));
		approval.setApproverUuid(manager.getUuid());
		approval.setPersonName(PersonService.getName(personService.getByUuid(order.getPersonUuid())));
		approval.setPersonUuid(order.getPersonUuid());
		approval.setUserId(order.getRequestedUserId());
		accountOrderApprovedService.save(approval);

		return new ResponseEntity<>(HttpStatus.OK);
	}


	private record OrgUnitManagerDto(String orgUnitUuid, String parentOrgUnitUuid, String orgUnitName, boolean managerExists, String managerUuid, String managerName, OrgUnitManagerSource source, boolean inherited) {}
	@GetMapping("/api/manager/orgUnitManagers")
	public ResponseEntity<?> getManagers() {
		var orgUnits = orgUnitService.getAll().stream().filter(o -> !o.isDeleted());
		var result = orgUnits.map(o -> new OrgUnitManagerDto(
				o.getUuid()
				,getValue(() -> o.getParent().getUuid())
				,o.getName()
				,o.getManager() != null
				,getValue(() -> o.getManager().getManagerUuid())
				,getValue(() -> o.getManager().getName())
				,getValue(() -> o.getManager().getSource())
				,getValue(() -> o.getManager().isInherited(),false)
			)).toList();
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	public record SetOrgUnitManagerDto(String orgUnitUuid, String managerUuid) {};
	@PostMapping("/api/manager/orgUnitManagers")
	public ResponseEntity<?> setOrgUnitManager(@RequestBody SetOrgUnitManagerDto setOrgUnitManagerDto ) {
		try {
			var orgUnit = orgUnitService.getByUuid(setOrgUnitManagerDto.orgUnitUuid);
			var changed = managerService.editSelectedManager(orgUnit, setOrgUnitManagerDto.managerUuid);
			if( changed ) {
				orgUnitService.save(orgUnit);
				orgUnitService.forceUpdateChildren(orgUnit);
			}
			return new ResponseEntity<>(HttpStatus.OK);
		}
		catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(),HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public record ManagerSuggestion(String uuid, String display) {}
	@GetMapping("/api/manager/searchManagers")
	public ResponseEntity<?> searchManagers(@RequestParam String query) {
		var validManagers = personService.searchValidMangers(query);
		var result = validManagers.stream().map(m -> new ManagerSuggestion(
				m.getUuid()
				,PersonService.getName(m) + " (" + AffiliationService.getPositionName(m.getPrimeAffiliation()) + " i " + m.getPrimeAffiliation().getOrgUnit().getName() + ")"
		)).toList();

		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	private boolean isAffiliationEditableByManager(Person manager, long affiliationId) {
		return getEditableAffiliationsForManager(manager).stream().anyMatch(a -> a.getId() == affiliationId);
	}

	private List<Affiliation> getEditableAffiliationsForManager(Person manager) {
		List<OrgUnit> managerOus = orgUnitService.getAllWhereManagerIs(manager);
		List<Affiliation> result = new ArrayList<>();
		for (OrgUnit ou : managerOus) {
			List<Affiliation> affiliationsForOu = affiliationService.findByOrgUnit(ou);
			result.addAll(affiliationsForOu.stream().filter(a -> !a.isDeleted() && a.getMaster().equals("SOFD")).toList());
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