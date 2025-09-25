package dk.digitalidentity.sofd.controller.mvc;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.dto.CreateAccountOrderDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.CreateDoubleAccountOrderDTO;
import dk.digitalidentity.sofd.dao.model.AccountOrder;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SupportedUserType;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderStatus;
import dk.digitalidentity.sofd.dao.model.enums.EndDate;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.EventType;
import dk.digitalidentity.sofd.log.AuditLogger;
import dk.digitalidentity.sofd.security.RequireControllerWriteAccess;
import dk.digitalidentity.sofd.service.AccountOrderService;
import dk.digitalidentity.sofd.service.AffiliationService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import dk.digitalidentity.sofd.service.UsernameGeneratorService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequireControllerWriteAccess
@Controller
public class AccountOrderController {

	@Autowired
	private AccountOrderService accountOrderService;

	@Autowired
	private AuditLogger auditLogger;
	
	@Autowired
	private PersonService personService;
	
	@Autowired
	private SupportedUserTypeService supportedUserTypeService;
	
	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired
	private UsernameGeneratorService usernameGeneratorService;

	@GetMapping("/ui/account/order/{uuid}/error/{cause}")
	public String errorPage(Model model, @PathVariable("uuid") String uuid, @PathVariable("cause") String cause) {
		model.addAttribute("uuid", uuid);
		model.addAttribute("cause", cause);

		return "accountorder/error";
	}

	@GetMapping("/ui/account/order/{personUUID}/{userType}")
	public String orderNewAccount(Model model, @PathVariable("personUUID") String personUUID, @PathVariable("userType") String userType) {
		Person person = personService.getByUuid(personUUID);
		if (person == null) {
			log.warn("Person does not exist: " + personUUID);
			return "redirect:/ui/person";
		}

		List<Affiliation> affiliations = new ArrayList<>();
		Set<String> userIds = new HashSet<>();
		
		SupportedUserType supportedUserType = supportedUserTypeService.findByKey(userType);
		if (supportedUserType == null) {
			log.error("Unknown userType for orderNewAccount flow: " + userType);
			return "redirect:/ui/person/view/" + person.getUuid();			
		}
		
		if (SupportedUserTypeService.isExchange(userType)) {
			userIds = accountOrderService.getActiveDirectoryUsersForExchangeAccount(person.getAffiliations());
			
			if (userIds.size() == 0) {
				log.warn("Person does not have any valid AD accounts for Exchange creation: " + personUUID);
				return "redirect:/ui/account/order/" + person.getUuid() + "/error/exchange";
			}
		}
		else if (SupportedUserTypeService.isOpus(userType)) {
			// we send OPUS affiliations for OPUS accounts
			affiliations = AffiliationService.notStoppedAffiliations(person.getAffiliations());
			affiliations = affiliations.stream().filter(u -> "OPUS".equals(u.getMaster())).collect(Collectors.toList());

			if (affiliations.size() == 0) {
				log.warn("Person does not have any OPUS affiliations: " + personUUID);
				return "redirect:/ui/account/order/" + person.getUuid() + "/error/affiliation";
			}
		}
		else if (!supportedUserType.isSingleUserMode()) {
			affiliations = AffiliationService.notStoppedAffiliations(person.getAffiliations());
			
			// only send affiliations from the wages system (and OS2sofd) in this scenario
			affiliations = affiliations.stream()
					.filter(u -> "SOFD".equals(u.getMaster()) || configuration.getModules().getLos().getPrimeAffiliationMaster().equals(u.getMaster()))
					.collect(Collectors.toList());

			if (affiliations.size() == 0) {
				log.warn("Person does not have any relevant affiliations: " + personUUID);
				return "redirect:/ui/account/order/" + person.getUuid() + "/error/affiliation";
			}
		}
		else if (configuration.getIntegrations().getOpus().isEnableActiveDirectoryEmployeeIdAssociation()) {
			// only send affiliations from the wages system in this scenario
			affiliations = AffiliationService.notStoppedAffiliations(person.getAffiliations());
			if (configuration.getIntegrations().getOpus().isAdEmployeeIdAssociationLimitedToPrimeAffiliation()) {
				affiliations = affiliations.stream().filter(u -> configuration.getModules().getLos().getPrimeAffiliationMaster().equals(u.getMaster())).collect(Collectors.toList());
			}

			// allow no-affiliation
			Affiliation affiliation = new Affiliation();
			affiliation.setEmployeeId("0");
			affiliation.setUuid("00000000-0000-4000-0000-000000000000");
			affiliation.setPositionName("Ikke tilknyttet noget tilhørsforhold");
			affiliations.add(0, affiliation);
		}
		
		CreateAccountOrderDTO accountOrder = CreateAccountOrderDTO.builder()
				.personUuid(personUUID)
				.personName(PersonService.getName(person))
				.userType(userType)
				.showEndDate(SupportedUserTypeService.getActiveDirectoryUserType().equals(userType))
				.affiliationUuid((affiliations.size() > 0) ? affiliations.get(0).getUuid() : "")
				.build();

		model.addAttribute("order", accountOrder);
		model.addAttribute("affiliations", affiliations);
		model.addAttribute("userIds", userIds);

		return "accountorder/order";
	}

	@GetMapping("/ui/account/order/{personUUID}/{userType}/double")
	public String orderNewDoubleAccount(Model model, @PathVariable("personUUID") String personUUID, @PathVariable("userType") String userType) {
		Person person = personService.getByUuid(personUUID);
		if (person == null) {
			log.warn("Person does not exist: " + personUUID);
			
			return "redirect:/ui/person";
		}
		
		if (!SupportedUserTypeService.isActiveDirectoryAndExchange(userType)) {
			log.error("Unknown userType for order Exchange and Active Directory flow: " + userType);

			return "redirect:/ui/person/view/" + person.getUuid();			
		}

		SupportedUserType adUserType = supportedUserTypeService.findByKey(SupportedUserTypeService.getActiveDirectoryUserType());
		if (adUserType == null) {
			log.error("No Active Directory user type");

			return "redirect:/ui/person/view/" + person.getUuid();	
		}
		
		SupportedUserType exchangeUserType = supportedUserTypeService.findByKey(SupportedUserTypeService.getExchangeUserType());
		if (exchangeUserType == null) {
			log.error("No Exchange user type");

			return "redirect:/ui/person/view/" + person.getUuid();	
		}
		
		List<Affiliation> adAffiliations = new ArrayList<>();

		// TODO: nedenstående logik er formodentligt en 99-100% kopi af den logik der ligger i almindelige konto-oprettelse, så
		//       det bør smides ud i nogle private metoder der kan håndtere det ensartet på tværs af de to metoder
		
		if (!adUserType.isSingleUserMode()) {
			adAffiliations = AffiliationService.notStoppedAffiliations(person.getAffiliations());
			
			// only send affiliations from the wages system (and OS2sofd) in this scenario
			adAffiliations = adAffiliations.stream()
					.filter(u -> "SOFD".equals(u.getMaster()) || configuration.getModules().getLos().getPrimeAffiliationMaster().equals(u.getMaster()))
					.collect(Collectors.toList());

			if (adAffiliations.size() == 0) {
				log.warn("Person does not have any relevant affiliations: " + personUUID);
				return "redirect:/ui/account/order/" + person.getUuid() + "/error/affiliation";
			}
		}
		else if (configuration.getIntegrations().getOpus().isEnableActiveDirectoryEmployeeIdAssociation()) {
			// only send affiliations from the wages system in this scenario
			adAffiliations = AffiliationService.notStoppedAffiliations(person.getAffiliations());
			if (configuration.getIntegrations().getOpus().isAdEmployeeIdAssociationLimitedToPrimeAffiliation()) {
				adAffiliations = adAffiliations.stream().filter(u -> configuration.getModules().getLos().getPrimeAffiliationMaster().equals(u.getMaster())).collect(Collectors.toList());
			}

			// allow no-affiliation
			Affiliation affiliation = new Affiliation();
			affiliation.setEmployeeId("0");
			affiliation.setUuid("00000000-0000-4000-0000-000000000000");
			affiliation.setPositionName("Ikke tilknyttet noget tilhørsforhold");
			adAffiliations.add(0, affiliation);
		}

		CreateDoubleAccountOrderDTO accountOrder = CreateDoubleAccountOrderDTO.builder()
				.personUuid(personUUID)
				.personName(PersonService.getName(person))
				.userType(userType)
				.adAffiliationUuid((adAffiliations.size() > 0) ? adAffiliations.get(0).getUuid() : "")
				.build();

		model.addAttribute("adPrettyName", supportedUserTypeService.getPrettyName(SupportedUserTypeService.getActiveDirectoryUserType()));
		model.addAttribute("exchangePrettyName", supportedUserTypeService.getPrettyName(SupportedUserTypeService.getExchangeUserType()));
		model.addAttribute("order", accountOrder);
		model.addAttribute("adAffiliations", adAffiliations);

		return "accountorder/order_double";
	}

	@PostMapping("/ui/account/order")
	public String createNewOrder(Model model, @ModelAttribute("order") CreateAccountOrderDTO order) {
		Person person = personService.getByUuid(order.getPersonUuid());
		if (person == null) {
			log.warn("Person does not exist: " + order.getPersonUuid());
			return "redirect:/ui/person";
		}
		
		SupportedUserType supportedUserType = supportedUserTypeService.findByKey(order.getUserType());
		if (supportedUserType == null) {
			log.warn("UserType does not exist: " + order.getUserType());
			return "redirect:/ui/person/view/" + person.getUuid();
		}
		
		// if an affiliationUuid is supplied, scan for it
		String employeeId = null;
		Affiliation triggerAffiliation = null;
		for (Affiliation affiliation : AffiliationService.notStoppedAffiliations(person.getAffiliations())) {
			if (affiliation.getUuid().equals(order.getAffiliationUuid())) {
				employeeId = affiliation.getEmployeeId();
				triggerAffiliation = affiliation;
				break;
			}
		}
				
		if (StringUtils.hasLength(order.getChosenUserId())) {
			order.setChosenUserId(order.getChosenUserId().trim());
		}
		else {
			String generatedUserId = usernameGeneratorService.getUsername(person, employeeId, order.getUserType(), order.getUserId(), triggerAffiliation);
			order.setChosenUserId(generatedUserId == null ? "" : generatedUserId);
		}
		
		// extra validation for exchange accounts
		if (SupportedUserTypeService.isExchange(order.getUserType())) {
			Set<String> userIds = accountOrderService.getActiveDirectoryUsersForExchangeAccount(person.getAffiliations());

			// if they have added an actual mail domain, trim it
			if (order.getChosenUserId().contains("@")) {
				order.setChosenUserId(order.getChosenUserId().substring(0, order.getChosenUserId().indexOf("@")));
			}
			
			if (!userIds.contains(order.getUserId())) {
				log.warn("Chosen userId is not valid for ordering an Exchange Account: " + order.getUserId());
				return "redirect:/ui/account/order/" + person.getUuid() + "/error/exchange";
			}
		}

		AccountOrder accountOrder = accountOrderService.createAccountOrder(
				person,
				supportedUserType,
				order.getChosenUserId(),
				order.getUserId(),
				employeeId,
				new Date(),
				(SupportedUserTypeService.getActiveDirectoryUserType().equals(supportedUserType.getKey()) ? order.getUserEndDate() : EndDate.NO),
				null,
				false,
				(StringUtils.hasLength(employeeId) ? true : false),
				!order.isRequireApproval(),
				true,
				triggerAffiliation);
		
		accountOrderService.save(accountOrder);

		return "redirect:/ui/person/view/" + person.getUuid();
	}
	
	@PostMapping("/ui/account/order/double")
	public String createNewDoubleOrder(Model model, @ModelAttribute("order") CreateDoubleAccountOrderDTO order) {
		Person person = personService.getByUuid(order.getPersonUuid());
		if (person == null) {
			log.warn("Person does not exist: " + order.getPersonUuid());

			return "redirect:/ui/person";
		}
		
		if (!SupportedUserTypeService.isActiveDirectoryAndExchange(order.getUserType())) {
			log.error("Unknown userType for order Exchange and Active Directory flow: " + order.getUserType());

			return "redirect:/ui/person/view/" + person.getUuid();			
		}
		
		SupportedUserType adUserType = supportedUserTypeService.findByKey(SupportedUserTypeService.getActiveDirectoryUserType());
		if (adUserType == null) {
			log.error("No Active Directory user type");
			
			return "redirect:/ui/person/view/" + person.getUuid();	
		}
		
		SupportedUserType exchangeUserType = supportedUserTypeService.findByKey(SupportedUserTypeService.getExchangeUserType());
		if (exchangeUserType == null) {
			log.error("No Exchange user type");
			
			return "redirect:/ui/person/view/" + person.getUuid();	
		}

		// TODO: nedenstående logik er formodentligt kopieret 99-100% fra den normale metode, og vi bør smide det ud i noget private hjælpe-metode

		// if an affiliationUuid is supplied, scan for it
		String employeeId = null;
		Affiliation triggerAffiliation = null;
		for (Affiliation affiliation : AffiliationService.notStoppedAffiliations(person.getAffiliations())) {
			if (affiliation.getUuid().equals(order.getAdAffiliationUuid())) {
				employeeId = affiliation.getEmployeeId();
				triggerAffiliation = affiliation;
				break;
			}
		}

		if (StringUtils.hasLength(order.getAdChosenUserId())) {
			order.setAdChosenUserId(order.getAdChosenUserId().trim());
		}
		else {
			String generatedUserId = usernameGeneratorService.getUsername(person, employeeId, adUserType.getKey(), null, triggerAffiliation);
			order.setAdChosenUserId(generatedUserId == null ? "" : generatedUserId);
		}

		if (StringUtils.hasLength(order.getExchangeChosenUserId())) {
			order.setExchangeChosenUserId(order.getExchangeChosenUserId().trim());
		}
		else {
			String generatedUserId = usernameGeneratorService.getUsername(person, employeeId, exchangeUserType.getKey(), order.getAdChosenUserId(), triggerAffiliation);
			order.setExchangeChosenUserId(generatedUserId == null ? "" : generatedUserId);
		}

		// if they have added an actual mail domain, trim it
		if (order.getExchangeChosenUserId().contains("@")) {
			order.setExchangeChosenUserId(order.getExchangeChosenUserId().substring(0, order.getExchangeChosenUserId().indexOf("@")));
		}
		
		AccountOrder adAccountOrder = accountOrderService.createAccountOrder(
				person,
				adUserType,
				order.getAdChosenUserId(),
				null,
				employeeId,
				new Date(),
				order.getAdUserEndDate(),
				null,
				false,
				(StringUtils.hasLength(employeeId)),
				!order.isRequireApproval(),
				true,
				triggerAffiliation);

		accountOrderService.save(adAccountOrder);
		
		AccountOrder exchangeAccountOrder = accountOrderService.createAccountOrder(
				person,
				exchangeUserType,
				order.getExchangeChosenUserId(),
				order.getAdChosenUserId(),
				employeeId,
				new Date(),
				EndDate.NO,
				null,
				false,
				(StringUtils.hasLength(employeeId)),
				true,
				true,
				triggerAffiliation);

		exchangeAccountOrder.setStatus(AccountOrderStatus.BLOCKED);
		exchangeAccountOrder.setDependsOn(adAccountOrder);
		accountOrderService.save(exchangeAccountOrder);

		return "redirect:/ui/report/accountorders";
	}

	@GetMapping("/ui/report/accountorders/retry/{id}/{userId:.+}")
	public String retryAccountOrder(Model model, @PathVariable("id") long id, @PathVariable("userId") String userId) {
		AccountOrder order = accountOrderService.findById(id);
		if (order != null && order.getStatus().equals(AccountOrderStatus.FAILED)) {

			// if they have added an actual mail domain, trim it
			if (userId.contains("@")) {
				userId = userId.substring(0, userId.indexOf("@"));
			}

			order.setRequestedUserId(userId);
			order.setMessage(null);
			order.setStatus(AccountOrderStatus.PENDING);
			accountOrderService.save(order);
		}
		
		String message = "Kontoordre genkørt: Handling: " + order.getOrderType() + " Type: " + order.getUserType() + " Status: " + order.getStatus() + ".";
		auditLogger.log(order.getPersonUuid(), EntityType.ACCOUNT_ORDER, EventType.RERUN, PersonService.getName(personService.getByUuid(order.getPersonUuid())), message);

		return "redirect:/ui/report/accountorders";
	}
	
}
