package dk.digitalidentity.sofd.controller.rest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.persistence.criteria.Predicate;
import javax.validation.Valid;

import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.history.Revision;
import org.springframework.data.jpa.datatables.mapping.Column;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.config.RoleConstants;
import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.datatables.dao.GridPersonDatatableDao;
import dk.digitalidentity.sofd.controller.mvc.datatables.dao.GridPersonDeletedDatatableDao;
import dk.digitalidentity.sofd.controller.mvc.datatables.dao.model.GridPerson;
import dk.digitalidentity.sofd.controller.mvc.datatables.dao.model.GridPersonActive;
import dk.digitalidentity.sofd.controller.mvc.dto.CprLookupDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.PrimeUserDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.ProfileDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.SimpleAffiliationDTO;
import dk.digitalidentity.sofd.controller.rest.model.ContactDTO;
import dk.digitalidentity.sofd.controller.rest.model.DisableAccountOrderDTO;
import dk.digitalidentity.sofd.controller.rest.model.LeaveDTO;
import dk.digitalidentity.sofd.controller.rest.model.PhoneDTO;
import dk.digitalidentity.sofd.dao.model.AccountOrder;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.FunctionType;
import dk.digitalidentity.sofd.dao.model.ModificationHistory;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.PersonLeave;
import dk.digitalidentity.sofd.dao.model.Phone;
import dk.digitalidentity.sofd.dao.model.SupportedUserType;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.UserChangeEmployeeIdQueue;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderType;
import dk.digitalidentity.sofd.dao.model.enums.EndDate;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.EventType;
import dk.digitalidentity.sofd.dao.model.enums.LeaveReason;
import dk.digitalidentity.sofd.dao.model.enums.PersonType;
import dk.digitalidentity.sofd.dao.model.mapping.PersonAuthorizationCodeMapping;
import dk.digitalidentity.sofd.dao.model.mapping.PersonPhoneMapping;
import dk.digitalidentity.sofd.log.AuditLogger;
import dk.digitalidentity.sofd.security.RequireControllerWriteAccess;
import dk.digitalidentity.sofd.security.RequirePersonCreaterOrControllerWriteAccess;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.security.RequireWriteContactInfoAccess;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.AccountOrderService;
import dk.digitalidentity.sofd.service.CprService;
import dk.digitalidentity.sofd.service.CprUpdateService;
import dk.digitalidentity.sofd.service.FunctionTypeService;
import dk.digitalidentity.sofd.service.ModificationHistoryService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import dk.digitalidentity.sofd.service.UserChangeEmployeeIdQueueService;
import dk.digitalidentity.sofd.service.UserService;
import dk.digitalidentity.sofd.service.model.ChangeType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequireReadAccess
@RestController
public class PersonRestController {

	@Autowired
	private PersonService personService;

	@Autowired
	private GridPersonDatatableDao personDatatableDao;

	@Autowired
	private GridPersonDeletedDatatableDao personDeletedDao;

	@Autowired
	private CprService cprService;

	@Autowired
	private UserService userService;

	@Autowired
	private AccountOrderService accountOrderService;

	@Autowired
	private FunctionTypeService functionTypeService;

	@Autowired
	private SupportedUserTypeService supportedUserTypeService;

	@Autowired
	private UserChangeEmployeeIdQueueService userChangeEmployeeIdQueueService;

	@Autowired
	private AuditLogger auditLogger;

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private ModificationHistoryService modificationHistoryService;

	@Autowired
	private CprUpdateService cprUpdateService;

	@Autowired
	MessageSource messageSource;

	@RequireControllerWriteAccess
	@PostMapping("/rest/person/{uuid}/leave")
	@ResponseBody
	public ResponseEntity<String> leave(@PathVariable("uuid") String uuid, @RequestBody LeaveDTO leaveDTO) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		Date startDate = leaveDTO.getLeaveStartDate();
		if (startDate == null) {
			startDate = new Date();
		}

		Date stopDate = leaveDTO.getLeaveStopDate();
		if (stopDate != null && stopDate.before(startDate)) {
			stopDate = startDate;
		}

		List<AccountOrder> newOrders = new ArrayList<AccountOrder>();
		
		if (leaveDTO.isLeave()) {
			if (person.getLeave() == null) {
				person.setLeave(new PersonLeave());

				// can only be modified on creation of leave
				person.getLeave().setExpireAccounts(leaveDTO.isExpireAccounts());
				if (leaveDTO.isDisableAccountOrders()) {
					person.getLeave().setDisableAccountOrders(true);
				}
				person.getLeave().setStartDate(startDate);

				if (leaveDTO.isExpireAccounts()) {
					// this is left commented out on purpose - this is NOT the same as setting the force-stop flag,
					// as this is just for a single-use deactivation of a SET of user accounts (not always all of them)
					// person.setForceStop(true);

					List<AccountOrder> orders = personService.generateExpireOrders(person, startDate, startDate);
					newOrders.addAll(orders);

					// the UI enforces this, but lets make sure
					leaveDTO.setDisableAccountOrders(true);
				}

				// the leaveForm can order the setting of this flag (it can be removed from the usual dialogue though
				// so this is just an easy way to do two things in one go)
				if (leaveDTO.isDisableAccountOrders()) {
					person.setDisableAccountOrdersCreate(true);
					person.setDisableAccountOrdersDisable(true);
					person.setDisableAccountOrdersDelete(true);
				}
			}

			// these are modifiable on existing leave data
			person.getLeave().setStopDate(stopDate);
			person.getLeave().setReason(leaveDTO.getReason());
			person.getLeave().setReasonText(leaveDTO.getReasonText());
		}
		else {
			List<AccountOrder> orders = personService.removeLeave(person);
			newOrders.addAll(orders);
		}

		// we have to loop (there won't be many) as the saveAll method has some nasty side-effects
		for (AccountOrder order : newOrders) {
			accountOrderService.save(order);
		}

		personService.save(person);
		
		String message;
		if (person.getLeave() == null) {
			message = "Pausemarkering fjernet via OS2sofd GUI";
		}
		else {
			var startDato = person.getLeave().getStartDate() != null ? new SimpleDateFormat("yyyy-MM-dd").format(person.getLeave().getStartDate()) : "ingen";
			var stopDato = person.getLeave().getStopDate() != null ? new SimpleDateFormat("yyyy-MM-dd").format(person.getLeave().getStopDate()) : "ingen";
			message = "Pausemarkering oprettet/redigere via OS2sofd GUI. Startdato: " + startDato + ", slutdato: " + stopDato;
		}
		auditLogger.log(person, EventType.PERSON_CHANGED, message);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequirePersonCreaterOrControllerWriteAccess
	@PostMapping("/rest/person/{uuid}/setEmployeeId/{userType}/{userId:.+}/{employeeId}")
	@ResponseBody
	public ResponseEntity<String> setEmployeeIdOnUser(@PathVariable("uuid") String uuid, @PathVariable("userType") String userType, @PathVariable("userId") String userId, @PathVariable("employeeId") String employeeIdInput, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
		// 0 = no employment selected
		var employeeId = "0".equals(employeeIdInput) ? null : employeeIdInput;
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			log.warn("Could not find person: " + uuid);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		User user = PersonService.getUsers(person).stream().filter(u -> Objects.equals(u.getUserType(), userType) && Objects.equals(u.getUserId(), userId)).findFirst().orElse(null);
		if (user == null) {
			log.warn("Could not find userId: " + userId + " of type " + userType);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if (employeeId != null) {
			Affiliation affiliation = person.getAffiliations().stream().filter(a -> a.getEmployeeId().equalsIgnoreCase(employeeId)).findFirst().orElse(null);
			if( affiliation == null ) {
				log.warn("Could not find affiliation with employeeId: " + employeeId);
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
			Set<String> constraintOUs = SecurityUtil.getOrgUnitUuidsConstraintFromPersonCreaterRole();
			if (!SecurityUtil.hasRole(RoleConstants.SYSTEM_ROLE_WRITE_ACCESS) && SecurityUtil.hasRole(RoleConstants.USER_ROLE_PERSON_CREATER) && !constraintOUs.isEmpty()) {
				if( !constraintOUs.contains(affiliation.getOrgUnit().getUuid()) ) {
					log.warn("PersonCreator has no access to affiliation with employeeId: " + employeeId);
					return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
				}
			}
		}

		try {
			personService.setEmployeeId(person, user, employeeId, date);
		} catch (Exception e) {
			return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
		}
		
		String message = "Tilførsfohold på " + supportedUserTypeService.getPrettyName(user.getUserType()) + " ændret til " + user.getUserId() + "/" + employeeId + ".";
		auditLogger.log(person.getUuid(), EntityType.PERSON, EventType.PERSON_CHANGED, PersonService.getName(person), message);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping("/rest/person/{uuid}/removefuturechange/{userType}/{userId}")
	@ResponseBody
	public ResponseEntity<String> setEmployeeIdOnUser(@PathVariable("uuid") String uuid, @PathVariable("userType") String userType, @PathVariable("userId") String userId) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			log.warn("Could not find person: " + uuid);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		User user = PersonService.getUsers(person).stream().filter(u -> Objects.equals(u.getUserType(), userType) && Objects.equals(u.getUserId(), userId)).findFirst().orElse(null);
		if (user == null) {
			log.warn("Could not find userId: " + userId + " of type " + userType);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		// only allow updates for AD users
		if (SupportedUserTypeService.isActiveDirectory(user.getUserType())) {
			UserChangeEmployeeIdQueue queue = userChangeEmployeeIdQueueService.findByUser(user);

			if (queue != null) {
				userChangeEmployeeIdQueueService.delete(queue);
			}
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping("/rest/person/{uuid}/forceStop")
	@ResponseBody
	public ResponseEntity<String> flipForceStopFlag(@PathVariable("uuid") String uuid, @RequestBody(required = false) String reason) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		person.setForceStop(!person.isForceStop());

		// follows forceStop setting
		person.setDisableAccountOrdersCreate(person.isForceStop());

		if (person.isForceStop()) {
			person.setStopReason(reason);
			person.setDisableAccountOrdersDisable(false);
		} else {
			person.setStopReason(null);
		}

		String[] userTypes = supportedUserTypeService.findAll().stream()
				.filter(u -> u.isCanOrder())
				.map(u -> u.getKey())
				.toArray(String[]::new);

		// generate deactivate orders
		// in the case stop is removed, any reactivate orders should be automatically generated by the normal person save logic or by nightly sync
		if (person.isForceStop()) {
			List<AccountOrder> deactivateOrders = personService.generateDeactivateOrders(person, userTypes, new Date());
			for (AccountOrder order : deactivateOrders) {
				accountOrderService.save(order);
			}
		}

		personService.save(person);

		auditLogger.log(person, EventType.PERSON_CHANGED, (person.isForceStop()) ? "STOP markering sat på person" : "STOP markering fjernet fra person");

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping("/rest/person/{uuid}/disableAccountOrders")
	@ResponseBody
	public ResponseEntity<String> flipDisableAccountOrders(@PathVariable("uuid") String uuid, @RequestBody DisableAccountOrderDTO disableAccountOrderDTO) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		var changes = false;
		if (person.isDisableAccountOrdersCreate() != disableAccountOrderDTO.isCreate()) {
			person.setDisableAccountOrdersCreate(disableAccountOrderDTO.isCreate());
			changes = true;
		}
		if (person.isDisableAccountOrdersDisable() != disableAccountOrderDTO.isDisable()) {
			person.setDisableAccountOrdersDisable(disableAccountOrderDTO.isDisable());
			changes = true;
		}
		if (person.isDisableAccountOrdersDelete() != disableAccountOrderDTO.isDelete()) {
			person.setDisableAccountOrdersDelete(disableAccountOrderDTO.isDelete());
			changes = true;
		}
		if (changes) {
			personService.save(person);
			var disableSettings = new ArrayList<String>();
			if (person.isDisableAccountOrdersCreate()) {
				disableSettings.add("opret");
			}
			if (person.isDisableAccountOrdersDisable()) {
				disableSettings.add("deaktiver");
			}
			if (person.isDisableAccountOrdersDelete()) {
				disableSettings.add("slet");
			}
			var auditMessage = disableSettings.isEmpty() ? "Alle undtagelsesmarkeringer fjernet" : "Undtagelsesmarkeringer sat (" + String.join(",", disableSettings) + ")";
			auditLogger.log(person, EventType.PERSON_CHANGED, auditMessage);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping("/rest/person/updatePrimaryAffiliations")
	@ResponseBody
	public ResponseEntity<String> updatePrimaryAffiliations(@RequestHeader("uuid") String uuid, @RequestBody List<SimpleAffiliationDTO> affiliationDTOs) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		// filter out non-prime, and ensure 1 affiliation is selected as prime
		affiliationDTOs = affiliationDTOs.stream().filter(a -> a.isPrime()).collect(Collectors.toList());
		if (affiliationDTOs.size() != 1) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		SimpleAffiliationDTO affiliationDTO = affiliationDTOs.get(0);

		if (person.getAffiliations() != null) {
			for (Affiliation affiliation : person.getAffiliations()) {
				if (affiliation.getId() == affiliationDTO.getId()) {
					affiliation.setSelectedPrime(true);
				} else {
					affiliation.setSelectedPrime(false);
				}
			}

			personService.save(person);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping("/rest/person/sofdUpdatePrimaryAffiliations")
	@ResponseBody
	public ResponseEntity<String> sofdUpdatePrimaryAffiliations(@RequestHeader("uuid") String uuid) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if (person.getAffiliations() != null) {
			for (Affiliation affiliation : person.getAffiliations()) {
				affiliation.setSelectedPrime(false);
			}

			// calling save() will ensure a prime is selected according to SOFD internal logic
			personService.save(person);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping("/rest/person/updateInheritPrivileges")
	@ResponseBody
	public ResponseEntity<String> updateInheritPrivileges(@RequestHeader("uuid") String uuid, @RequestBody List<SimpleAffiliationDTO> affiliationDTOs) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if (person.getAffiliations() != null) {
			for (Affiliation affiliation : person.getAffiliations()) {
				Optional<SimpleAffiliationDTO> optAffiliationDTO = affiliationDTOs.stream().filter(dto -> dto.getId() == affiliation.getId()).findAny();
				if (optAffiliationDTO.isPresent()) {
					affiliation.setInheritPrivileges(optAffiliationDTO.get().isInheritPrivileges());
				}
			}

			personService.save(person);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping("/rest/person/deleteUser/{userType}/{userId:.+}")
	@ResponseBody
	public ResponseEntity<String> deleteUser(@PathVariable("userType") String userType, @PathVariable("userId") String userId) {
		if (supportedUserTypeService.findByKey(userType) == null) {
			log.warn("Unknown usertype: " + userType);

			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		User user = userService.findByUserIdAndUserType(userId, userType);
		if (user == null) {
			log.warn("Could not find user: " + userId + " / " + userType);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if (user.isDisabled() == true) {
			log.warn("User not active: " + userId + " / " + userType);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		Person person = personService.findByUser(user);
		if (person == null) {
			log.warn("User account did not have any Person associated: " + userId + " / " + userType);

			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		deactivateAndDeleteAccount(person, user);

		String message = "Konto " + supportedUserTypeService.getPrettyName(user.getUserType()) + " bestilt deaktiveret/slettet.";
		auditLogger.log(person.getUuid(), EntityType.PERSON, EventType.USER_DELETED, PersonService.getName(person), message);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping("/rest/person/reactivateUser/{userType}/{userId:.+}")
	@ResponseBody
	public ResponseEntity<String> reactivateUser(@PathVariable("userType") String userType, @PathVariable("userId") String userId) {
		if (supportedUserTypeService.findByKey(userType) == null) {
			log.warn("Unknown usertype: " + userType);

			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		User user = userService.findByUserIdAndUserType(userId, userType);
		if (user == null) {
			log.warn("Could not find user: " + userId + " / " + userType);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if (user.isDisabled() == false) {
			log.warn("User not deactivated: " + userId + " / " + userType);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		Person person = personService.findByUser(user);
		if (person == null) {
			log.warn("User account did not have any Person associated: " + userId + " / " + userType);

			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		reactivateAccount(person, user);
		
		// remove any pending deactivate/delete orders
		List<AccountOrder> pendingOrders = accountOrderService.findPendingByUserTypeAndUserId(userType, userId);
		for (AccountOrder order : pendingOrders) {
			accountOrderService.delete(order);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	private void reactivateAccount(Person person, User user) {
		String userId = user.getUserId();
		String linkedUserId = null;

		if (SupportedUserTypeService.isExchange(user.getUserType())) {
			userId = userId.split("@")[0];
			linkedUserId = user.getMasterId();
		}

		AccountOrder order = accountOrderService.createAccountOrder(
				person,
				supportedUserTypeService.findByKey(user.getUserType()),
				userId,
				linkedUserId,
				user.getEmployeeId(),
				null,
				EndDate.NO,
				null,
				false,
				configuration.getModules().getAccountCreation().isForceSetEmployeeId(),
				true,
				true,
				null);

		accountOrderService.save(order);
	}

	private void deactivateAndDeleteAccount(Person person, User user) {
		AccountOrder order = accountOrderService.deactivateOrDeleteAccountOrder(AccountOrderType.DEACTIVATE, person, user.getEmployeeId(), user.getUserType(), user.getUserId());

		// exchange accounts are linked to an AD account that needs to be updated
		if (SupportedUserTypeService.isExchange(user.getUserType())) {
			order.setLinkedUserId(user.getMasterId());
		}

		accountOrderService.save(order);

		SupportedUserType supportedUserType = supportedUserTypeService.findByKey(user.getUserType());
		if (supportedUserType.isDeleteEnabled()) {
			Date date = LocalDate.now().plusDays((int) supportedUserType.getDaysToDelete()).toDate();

			order = accountOrderService.deactivateOrDeleteAccountOrder(AccountOrderType.DELETE, person, user.getEmployeeId(), user.getUserType(), user.getUserId(), date);
			accountOrderService.save(order);
		}
	}

	private record CprDTO(String cpr) { }
	@RequirePersonCreaterOrControllerWriteAccess
	@PostMapping(value = "/rest/person/getByCPR", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ResponseEntity<CprLookupDTO> getByCPR(@RequestBody CprDTO cprDTO) {
		Person person = personService.findByCpr(cprDTO.cpr());
		if (person != null) {
			return new ResponseEntity<>(HttpStatus.CONFLICT);
		}

		CprLookupDTO cprLookupDTO = cprService.getByCpr(cprDTO.cpr(), true);

		if (cprLookupDTO != null) {
			return new ResponseEntity<>(cprLookupDTO, HttpStatus.OK);
		}

		return new ResponseEntity<>(HttpStatus.NOT_FOUND);
	}

	@RequireControllerWriteAccess
	@PostMapping("/rest/person/updateFromCPR")
	@ResponseBody
	public ResponseEntity<HttpStatus> updateFromCPR(@RequestHeader("uuid") String uuid) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		cprUpdateService.updatePerson(person.getUuid());
		
		String message = "CPR-opdatinger af " + PersonService.getName(person) + " (" + person.getCprMaskSuffix() + ").";
		auditLogger.log(person.getUuid(), EntityType.PERSON, EventType.PERSON_CHANGED, PersonService.getName(person), message);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping("/rest/person/taxedPhone/{uuid}")
	@ResponseBody
	public ResponseEntity<HttpStatus> updatePhoneTax(@PathVariable("uuid") String uuid, @RequestHeader("checked") boolean checked) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		person.setTaxedPhone(checked);
		personService.save(person);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping("/rest/person/profile/update")
	public HttpEntity<?> updateProfile(@RequestBody @Valid ProfileDTO profileDTO, BindingResult bindingResult) throws Exception {
		if (bindingResult.hasErrors()) {
			return new ResponseEntity<>(bindingResult.getAllErrors(), HttpStatus.BAD_REQUEST);
		}

		String uuid = profileDTO.getPersonUuid();
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			log.warn("Could not find person with uuid " + uuid + " while updating profile.");
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		boolean changes = false;
		if (!Objects.equals(person.getChosenName(), profileDTO.getChosenName())) {
			person.setChosenName(profileDTO.getChosenName());
			changes  = true;
		}

		if (!Objects.equals(person.getFirstname(), profileDTO.getFirstname())) {
			person.setFirstname(profileDTO.getFirstname());
			changes = true;
		}

		if (!Objects.equals(person.getSurname(), profileDTO.getSurname())) {
			person.setSurname(profileDTO.getSurname());
			changes = true;
		}

		if (!Objects.equals(person.getPersonType(), profileDTO.getPersonType())) {
			person.setPersonType(profileDTO.getPersonType());
			changes = true;
		}

		if (changes) {
			person = personService.save(person);
		}
		
		String message = "Stamdata for " + PersonService.getName(person) + " (" + person.getCprMaskSuffix() + ") redigeret.";
		auditLogger.log(person.getUuid(), EntityType.PERSON, EventType.PERSON_CHANGED, PersonService.getName(person), message);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping("/rest/person/editPhone")
	@ResponseBody
	public ResponseEntity<HttpStatus> editPhone(@RequestHeader("uuid") String uuid, @RequestBody PhoneDTO phoneDTO) {
		if (!StringUtils.hasLength(phoneDTO.getPhoneNumber())) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		FunctionType functionType = functionTypeService.findById(phoneDTO.getFunctionType());

		if (phoneDTO.getId() == 0) {
			Phone phone = new Phone();
			phone.setMaster("SOFD");
			phone.setMasterId(UUID.randomUUID().toString());
			phone.setPhoneNumber(phoneDTO.getPhoneNumber());
			phone.setPhoneType(phoneDTO.getPhoneType());
			phone.setNotes(phoneDTO.getNotes());
			phone.setVisibility(phoneDTO.getVisibility());
			phone.setPrime(false);
			phone.setTypePrime(false);
			phone.setFunctionType(functionType);

			PersonPhoneMapping mapping = new PersonPhoneMapping();
			mapping.setPerson(person);
			mapping.setPhone(phone);

			person.getPhones().add(mapping);
		}
		else {
			List<Phone> phones = PersonService.getPhones(person);
			Optional<Phone> existingPhone = phones.stream().filter(p -> p.getId() == phoneDTO.getId()).findFirst();
			if (existingPhone.isPresent()) {
				Phone modifiedPhone = existingPhone.get();

				modifiedPhone.setPhoneNumber(phoneDTO.getPhoneNumber());
				modifiedPhone.setPhoneType(phoneDTO.getPhoneType());
				modifiedPhone.setNotes(phoneDTO.getNotes());
				modifiedPhone.setVisibility(phoneDTO.getVisibility());
				modifiedPhone.setFunctionType(functionType);
			}
			else {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
		}

		personService.save(person);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping("/rest/person/deletePhone")
	@ResponseBody
	public ResponseEntity<HttpStatus> deletePhone(@RequestHeader("uuid") String uuid, @RequestHeader("id") long id) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		boolean removedAny = person.getPhones().removeIf(phoneMapping -> phoneMapping.getPhone().getId() == id);

		personService.save(person);

		return removedAny ? new ResponseEntity<>(HttpStatus.OK) : new ResponseEntity<>(HttpStatus.BAD_REQUEST);
	}

	// read access is fine here, used by datatables
	@SuppressWarnings("unchecked")
	@PostMapping("/rest/person/list")
	public DataTablesOutput<GridPerson> list(@Valid @RequestBody DataTablesInput input, BindingResult bindingResult, @RequestHeader("show-inactive") boolean showInactive, Locale locale) {
		if (bindingResult.hasErrors()) {
			DataTablesOutput<GridPerson> error = new DataTablesOutput<>();
			error.setError(bindingResult.toString());

			return error;
		}

		DataTablesOutput<?> result = new DataTablesOutput<>();
		boolean userHasCPRAccess = SecurityUtil.getUserRoles().contains(RoleConstants.USER_ROLE_CPR_ACCESS);
		if (userHasCPRAccess) {
			input.addColumn("cpr", true, false, "");
		}
		String searchValue = "";
		if (input != null && input.getColumns() != null) {
			Optional<Column> obsColumnOpt = input.getColumns().stream().filter(column -> "obs".equalsIgnoreCase(column.getName())).findAny();
			if (obsColumnOpt.isPresent()) {
				Column obsColumn = obsColumnOpt.get();
				if (obsColumn.getSearch() != null && obsColumn.getSearch().getValue() != null && !obsColumn.getSearch().getValue().isEmpty()) {
					searchValue = obsColumn.getSearch().getValue();
					obsColumn.getSearch().setValue("");
				}
			}
		}

		if (showInactive) {
			result = personDeletedDao.findAll(input);
		}
		else {
			var activeResult = personDatatableDao.findAll(input, getObsByInput(searchValue, locale));
			activeResult.getData().forEach(y -> y.setReasonTranslated(y.getReason() == null ? "" : messageSource.getMessage(y.getReason().getMessage(), null, locale)));
			result = activeResult;
		}

		DataTablesOutput<GridPerson> output = new DataTablesOutput<>();
		output.setDraw(result.getDraw());
		output.setRecordsFiltered(result.getRecordsFiltered());
		output.setRecordsTotal(result.getRecordsTotal());
		output.setError(result.getError());
		output.setData((List<GridPerson>) result.getData());

		return output;
	}

	private Specification<GridPersonActive> getObsByInput(String searchValue, Locale locale) {
		// Lowercase the input, just to be consistent
		String input = searchValue.toLowerCase();
		Specification<GridPersonActive> specification = (root, query, criteriaBuilder) -> {
			if( !StringUtils.hasText(input)) {
				return criteriaBuilder.and();
			}
			Predicate finalPredicate = criteriaBuilder.or();

			var leavePredicate = criteriaBuilder.equal(root.get("leave"), true);
			if ("pause".startsWith(input) || "på pause".startsWith(input)) {
				finalPredicate = criteriaBuilder.or(finalPredicate,leavePredicate);
			}
			for (LeaveReason value : LeaveReason.values()) {
				var translatedReason = messageSource.getMessage(value.getMessage(), null, locale).toLowerCase();
                if (translatedReason.startsWith(input)) {
					var leaveReasonPredicate = criteriaBuilder.and(leavePredicate,criteriaBuilder.equal(root.get("reason"), value));
					finalPredicate = criteriaBuilder.or(finalPredicate,leaveReasonPredicate);
                }
			}

			if ("stoppet".startsWith(input)) {
				var stopPredicate = criteriaBuilder.equal(root.get("forceStop"), true);
				finalPredicate = criteriaBuilder.or(finalPredicate,stopPredicate);
			}

			if ("undtaget".startsWith(input) || "undtagelsmarkeringer".startsWith(input)) {
				var idmPredicate = criteriaBuilder.or(criteriaBuilder.equal(root.get("disableAccountOrdersCreate"), true), criteriaBuilder.equal(root.get("disableAccountOrdersDelete"), true), criteriaBuilder.equal(root.get("disableAccountOrdersDisable"), true));
				finalPredicate = criteriaBuilder.or(finalPredicate,idmPredicate);
			}

			if ("ugyldig civilstand".startsWith(input) || "civilstand".startsWith(input)) {
				var deadPredicate = criteriaBuilder.equal(root.get("dead"), true);
				finalPredicate = criteriaBuilder.or(finalPredicate,deadPredicate);
			}

			if ("umyndiggjort".startsWith(input)) {
				var disenfranchisedPredicate = criteriaBuilder.equal(root.get("disenfranchised"), true);
				finalPredicate = criteriaBuilder.or(finalPredicate,disenfranchisedPredicate);
			}

			if ("fiktivt cpr".startsWith(input) || "cpr".startsWith(input)) {
				var fictivePredicate = criteriaBuilder.equal(root.get("fictiveCpr"), true);
				finalPredicate = criteriaBuilder.or(finalPredicate,fictivePredicate);
			}

			if ("robot".startsWith(input)) {
				var robotPredicate = criteriaBuilder.equal(root.get("personType"), PersonType.ROBOT);
				finalPredicate = criteriaBuilder.or(finalPredicate,robotPredicate);
			}

			return finalPredicate;
		};
		return specification;
	}

	@RequireControllerWriteAccess
	@PostMapping("/rest/person/updatePrimaryUserAccounts")
	@ResponseBody
	public ResponseEntity<String> updatePrimaryUserAccounts(@RequestHeader("uuid") String uuid, @RequestBody List<PrimeUserDTO> primeUserDTOs ) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		Map<String, List<PrimeUserDTO>> users = primeUserDTOs.stream()
				.collect(Collectors.groupingBy(PrimeUserDTO::getUserType));

		// Check if data is correct
		for (String type : users.keySet()) {
			if (users.get(type).stream().filter(u -> u.isPrime()).count() != 1) {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
		}

		for (User user : PersonService.getUsers(person)) {
			Optional<PrimeUserDTO> userDTO = primeUserDTOs.stream().filter(dto -> Objects.equals(dto.getUuid(), user.getUuid())).findAny();

			if (userDTO.isPresent()) {
				user.setPrime(userDTO.get().isPrime());
			}
		}

		personService.save(person);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping("/rest/person/updatePrimaryPhones")
	@ResponseBody
	public ResponseEntity<String> updatePrimaryPhones(@RequestHeader("uuid") String uuid, @RequestBody List<PhoneDTO> phoneDTOs ) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		for (PhoneDTO phoneDTO : phoneDTOs) {
			Optional<Phone> first = PersonService.getPhones(person).stream().filter(p -> p.getId() == phoneDTO.getId()).findFirst();
			if (first.isPresent()) {
				Phone phone = first.get();

				phone.setPrime(phoneDTO.isPrime());
				phone.setTypePrime(phoneDTO.isTypePrime());
			}
			else {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
		}

		personService.save(person);
		return new ResponseEntity<>(HttpStatus.OK);
	}


	@RequireWriteContactInfoAccess
	@PostMapping("/rest/person/update/contacts")
	@ResponseBody
	public HttpEntity<ContactDTO> updateContactInfo(@RequestHeader("uuid") String uuid, @RequestBody ContactDTO contactInfo) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		person.setKeyWords(contactInfo.getKeywords());
		person.setNotes(contactInfo.getNotes());
		personService.save(person);

		return new ResponseEntity<>(contactInfo, HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping("/rest/person/updatePrimaryAuthorizationCode/{uuid}/{authorizationCodeId}")
	@ResponseBody
	public ResponseEntity<String> updatePrimaryAuthorizationCode(@PathVariable("uuid") String uuid, @PathVariable("authorizationCodeId") long authorizationCodeId) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if (person.getAuthorizationCodes() == null || person.getAuthorizationCodes().stream().noneMatch(ac -> ac.getAuthorizationCode().getId() == authorizationCodeId)) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		for (PersonAuthorizationCodeMapping mapping : person.getAuthorizationCodes()) {
			mapping.getAuthorizationCode().setPrime(mapping.getAuthorizationCode().getId() == authorizationCodeId);
		}

		personService.save(person);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping("/rest/person/{uuid}/poke")
	@ResponseBody
	public ResponseEntity<String> poke(@PathVariable("uuid") String uuid) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		ModificationHistory modificationHistory = new ModificationHistory();
		modificationHistory.setEntity(EntityType.PERSON);
		modificationHistory.setUuid(person.getUuid());
		modificationHistory.setChanged(new Date());
		modificationHistory.setChangeType(ChangeType.UPDATE);

		modificationHistoryService.insert(modificationHistory);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	public record DiffRecord(String left, String right) { }
	@GetMapping(value = "/rest/person/revision/diff/{uuid}")
	@ResponseBody
	public ResponseEntity<DiffRecord> downloadPersonHistory(@PathVariable("uuid") String uuid, @RequestParam("leftRevId") Integer leftRevId, @RequestParam("rightRevId") Integer rightRevId) {
		String leftJson;
		String rightJson;
		try {
			Revision<Integer, Person> leftRevision = personService.findRevision(uuid, leftRevId);
			leftJson = personService.getRevisionJson(leftRevision);
		} catch (Exception e) {
			log.warn("downloadPersonHistory failed when trying to find left revision with id: " + leftRevId + ", error message: " + e.getMessage() + ".");
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		
		try {
			Revision<Integer, Person> rightRevision = personService.findRevision(uuid, rightRevId);
			rightJson = personService.getRevisionJson(rightRevision);
		} catch (Exception e) {
			log.warn("downloadPersonHistory failed when trying to find right revision with id: " + rightRevId + ", error message: " + e.getMessage() + ".");
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
		var result = new DiffRecord(leftJson,rightJson);
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
}
