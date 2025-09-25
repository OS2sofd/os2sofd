package dk.digitalidentity.sofd.controller.mvc;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import dk.digitalidentity.sofd.controller.mvc.dto.PwdLockedDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.PwdReminderDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.RecipientType;
import dk.digitalidentity.sofd.controller.mvc.dto.SMSRecipientDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.SearchForm;
import dk.digitalidentity.sofd.controller.mvc.dto.enums.SMSChannel;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.Phone;
import dk.digitalidentity.sofd.dao.model.SmsLog;
import dk.digitalidentity.sofd.dao.model.SmsLogRecipient;
import dk.digitalidentity.sofd.dao.model.enums.PhoneType;
import dk.digitalidentity.sofd.security.RequireSMSAccess;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.AffiliationService;
import dk.digitalidentity.sofd.service.EmailService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.PwdLockedService;
import dk.digitalidentity.sofd.service.PwdReminderService;
import dk.digitalidentity.sofd.service.RoleCatalogueService;
import dk.digitalidentity.sofd.service.SMSService;
import dk.digitalidentity.sofd.service.SmsLogService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import dk.digitalidentity.sofd.service.TemplateService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequireSMSAccess
@Controller
public class SMSController {

	@Autowired
	private OrgUnitService orgUnitService;

	@Autowired
	private RoleCatalogueService roleCatalogService;

	@Autowired
	private AffiliationService affiliationService;

	@Autowired
	private SMSService smsService;

	@Autowired
	private TemplateService templateService;

	@Autowired
	private PersonService personService;

	@Autowired
	private PwdReminderService pwdReminderService;

	@Autowired
	private PwdLockedService pwdLockedService;

	@Autowired
	private SmsLogService smsLogService;

    @Autowired
    private EmailService emailService;

	@GetMapping("/ui/sms/log")
	public String smsLog(Model model) {
		model.addAttribute("logs", smsLogService.findAll());

		return "sms/smslog";
	}

	@GetMapping("/ui/sms/log/{id}")
	public String smsLogRecipients(Model model, @PathVariable("id") Long id) {
		Optional<SmsLog> logOptional = smsLogService.getById(id);

		if (logOptional.isPresent()) {
			model.addAttribute("log", logOptional.get());
		} else {
			log.error("SMS Log not found.");
			return "redirect:/ui/sms/log";
		}

		return "sms/recipients";
	}

	@GetMapping("/ui/sms/message")
	public String createMessage(Model model) {
		model.addAttribute("templates", templateService.findAll());

		return "sms/message";
	}

	@PostMapping("/ui/sms/message")
	public String createMessagePost(Model model, String selectedMessage, String messageType, HttpServletRequest httpServletRequest) {
		httpServletRequest.getSession().setAttribute("selectedMessage", selectedMessage);
		model.addAttribute("allItSystems", roleCatalogService.fetchAllItSystems());
		model.addAttribute("allOrgUnits", orgUnitService.getAllTree());
		model.addAttribute("allOrgUnitsV2", orgUnitService.getAllTreeByAddress());
		model.addAttribute("searchForm", new SearchForm());

		return "sms/search";
	}

	@PostMapping("/ui/sms/filter")
	public String filter(Model model, SearchForm searchForm, HttpServletRequest httpServletRequest) throws Exception {
		model.addAttribute("recipients", selectRecipients(searchForm));
		return "sms/filter";
	}

	private void updateRecipientMap(HashMap<String,SMSRecipientDTO> recipients, OrgUnit orgUnit, Person person, boolean onlyTypePrime, SMSRecipientDTO.RecipientCriteria criteria, SMSChannel channel) throws Exception {
		// ensure either orgUnit or person is supplied, but not both
		if ((orgUnit == null && person == null) || (orgUnit != null && person != null)) {
			throw new Exception("Unexpected input");
		}
		var isOrgUnit = orgUnit != null;
		var uuid = isOrgUnit ? orgUnit.getUuid() : person.getUuid();
		var recipient = recipients.get(uuid);
		if( recipient == null)
		{
			recipient = new SMSRecipientDTO();
			recipient.setUuid(uuid);
			recipient.setName(isOrgUnit ? orgUnit.getName() : PersonService.getName(person));
			recipient.setType(isOrgUnit ? RecipientType.ORGUNIT : RecipientType.PERSON);
			if( Set.of(SMSChannel.EMAIL,SMSChannel.EMAIL_OR_SMS).contains(channel) ) {
				recipient.setEmail(isOrgUnit ? orgUnit.getEmail() : PersonService.getEmail(person));
			}
			if( channel == SMSChannel.SMS || (channel == SMSChannel.EMAIL_OR_SMS && !StringUtils.hasLength(recipient.getEmail()))) {
				var phones = (isOrgUnit
						? OrgUnitService.getPhones(orgUnit).stream().filter(p -> p.getPhoneType() == PhoneType.MOBILE && (p.isTypePrime() || !onlyTypePrime))
						: PersonService.getPhones(person).stream().filter(p -> p.getPhoneType().equals(PhoneType.MOBILE) && p.isTypePrime())).map(Phone::getPhoneNumber).distinct().collect(Collectors.toSet());
				recipient.setPhones(phones);
			}
		}

		recipient.getCriteria().add(criteria);
		// only add recipient if it has either email or phones
		if( StringUtils.hasLength(recipient.getEmail()) || (recipient.getPhones() != null && !recipient.getPhones().isEmpty())) {
			recipients.put(recipient.getUuid(),recipient);
		}
	}

	private Map<String, SMSRecipientDTO> selectRecipients(SearchForm searchForm) throws Exception {
		var recipients = new HashMap<String, SMSRecipientDTO>();
		var isFilteringOnOrgUnits = searchForm.getOrgUnits() != null && !searchForm.getOrgUnits().isEmpty();
		var isFilteringOnFunctions = !"NONE".equals(searchForm.getFunction());
		var isFilteringOnItSystems = searchForm.getItSystems() != null && !searchForm.getItSystems().isEmpty();

		// search by OrgUnit selection
		List<OrgUnit> orgUnits = new ArrayList<>();
		if (isFilteringOnOrgUnits) {
			// filter out duplicates
			Set<String> ouUuids = new HashSet<>(searchForm.getOrgUnits());

			for (String uuid : ouUuids) {
				OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
				if (orgUnit == null) {
					log.warn("Unable to find OU with UUID: " + uuid + ". Skipping.");
					continue;
				}
				orgUnits.add(orgUnit);
			}
		}
		else {
			orgUnits = orgUnitService.getAllActive();
		}

		for (OrgUnit orgUnit : orgUnits) {
			List<Affiliation> affiliations = affiliationService.findByCalculatedOrgUnitAndActive(orgUnit);
			for (Affiliation affiliation : affiliations) {
				updateRecipientMap(recipients, null, affiliation.getPerson(),true, SMSRecipientDTO.RecipientCriteria.ORGUNIT,searchForm.getChannel());
			}

			// we only include orgUnits if we are not filtering on it-systems and/or functions
			if ( searchForm.isIncludeOrgUnits() && !isFilteringOnItSystems && !isFilteringOnFunctions) {
				updateRecipientMap(recipients,orgUnit,null,false, SMSRecipientDTO.RecipientCriteria.ORGUNIT,searchForm.getChannel());
			}
		}

		// search by function
		if( isFilteringOnFunctions ) {
			List<Person> functionPersons = switch (searchForm.getFunction()) {
				case "LEDER" -> personService.findAllManagers();
				case "TR" -> personService.findAllTRs();
				case "SR" -> personService.findAllSRs();
				default -> new ArrayList<>();
			};
			for (var functionPerson : functionPersons) {
				updateRecipientMap(recipients, null, functionPerson,true, SMSRecipientDTO.RecipientCriteria.FUNCTION,searchForm.getChannel());
			}
		}

		// search by it-systems
		if( isFilteringOnItSystems ) {
			List<Person> itSystemPersons = new ArrayList<>();
			if (searchForm.getItSystems() != null && !searchForm.getItSystems().isEmpty()) {
				Set<String> userIds = new HashSet<>();
				for (String itSystemIdentifier : searchForm.getItSystems()) {
					Set<String> userIdsFromItSystem = roleCatalogService.getUsersByItSystem(itSystemIdentifier);
					userIds.addAll(userIdsFromItSystem);
				}
				itSystemPersons = personService.getAll().stream().filter(p -> PersonService.getUsers(p).stream()
						.filter(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType())).anyMatch(u -> userIds.contains(u.getUserId()))).toList();
			}
			for (var itSystemPerson : itSystemPersons) {
				updateRecipientMap(recipients, null, itSystemPerson,true, SMSRecipientDTO.RecipientCriteria.IT_SYSTEM,searchForm.getChannel());
			}
		}

		// if AND-criteria is used, make sure to remove any recipients that does not meet all criteria
		if (searchForm.getOperation().equals("AND")) {
			for (var r : recipients.values().stream().toList()) {
				if (isFilteringOnOrgUnits && !r.getCriteria().contains(SMSRecipientDTO.RecipientCriteria.ORGUNIT)
						|| isFilteringOnFunctions && !r.getCriteria().contains(SMSRecipientDTO.RecipientCriteria.FUNCTION)
						|| isFilteringOnItSystems && !r.getCriteria().contains(SMSRecipientDTO.RecipientCriteria.IT_SYSTEM)) {
					recipients.remove(r.getUuid());
				}
			}
		}
		return recipients;
	}

	@PostMapping("/ui/sms/sendmessage")
	public ResponseEntity<?> sendMessage(@RequestBody HashMap<String,SMSRecipientDTO> recipients, HttpServletRequest httpServletRequest) {
		String message = (String) httpServletRequest.getSession().getAttribute("selectedMessage");
		if (!StringUtils.hasLength(message)) {
			return ResponseEntity.badRequest().body("Ingen valgt besked");
		}

		var selectedRecipients = recipients.values().stream().filter(SMSRecipientDTO::isSelected).toList();
		var distinctEmails = selectedRecipients.stream().filter(r -> r.getEmail() != null).map(SMSRecipientDTO::getEmail).distinct().collect(Collectors.toSet());
		var distinctPhones = selectedRecipients.stream().filter(r -> r.getPhones() != null).flatMap(r -> r.getPhones().stream()).distinct().collect(Collectors.toSet());

		if( distinctEmails.size() + distinctPhones.size() == 0) {
			return ResponseEntity.badRequest().body("Ingen modtagere fundet");
		}

		var smsLog = new SmsLog();
		smsLog.setRecipients(new HashSet<>());
		smsLog.setMessage(message);
		smsLog.setUserId(SecurityUtil.getUser());

		// send all sms messages as one
		smsService.sendMessage(message, distinctPhones);

		// send all emails
		for(var email : distinctEmails) {
			emailService.sendMessage(email, "Mail fra " + SecurityUtil.getUser(), message, null, null, null, "Sendt via OS2sofd kommunikationsmodul");
		}

		// add recipient logs
		for( var recipient : recipients.values() ) {
			var smsLogRecipient = new SmsLogRecipient();
			smsLogRecipient.setName(recipient.getName());
			smsLogRecipient.setEmail(recipient.getEmail());
			var phone = recipient.getPhones() == null ? null : String.join(", ", recipient.getPhones());
			smsLogRecipient.setPhone(phone);
			smsLogRecipient.setType(recipient.getType());
			smsLogRecipient.setSmsLog(smsLog);
			smsLog.getRecipients().add(smsLogRecipient);
		}
		smsLogService.save(smsLog);

		return ResponseEntity.ok().body("");
	}

	@GetMapping("/ui/sms/pwdreminders")
	public String pwdReminderRules(Model model) {
		PwdReminderDTO form = new PwdReminderDTO();
		form.setDays(pwdReminderService.getPwdReminderDaysBefore());
		form.setEmailTxt(pwdReminderService.getPwdReminderEmailTxt());
		form.setEmailSubject(pwdReminderService.getPwdReminderEmailSubject());
		form.setSmsTxt(pwdReminderService.getPwdReminderSmsTxt());
		form.setStrategy(pwdReminderService.getPwdReminderStrategy());
		form.setTime(pwdReminderService.getPwdReminderTime().toString());
		form.setOrgUnits(pwdReminderService.getPwdReminderOrgUnitFilter());

		model.addAttribute("allOrgUnits", orgUnitService.getAllTree());
		model.addAttribute("reminderForm", form);

		return "sms/pwdreminders";
	}

	@PostMapping("/ui/sms/pwdreminders")
	public String pwdReminderRules(Model model, PwdReminderDTO form) {
		pwdReminderService.setPwdReminderEmailTxt(form.getEmailTxt());
		pwdReminderService.setPwdReminderEmailSubject(form.getEmailSubject());
		pwdReminderService.setPwdReminderSmsTxt(form.getSmsTxt());
		pwdReminderService.setPwdReminderStrategy(form.getStrategy());
		pwdReminderService.setPwdReminderOrgUnitFilter(form.getOrgUnits());

		pwdReminderService.setPwdReminderDaysBefore(form.getDays());
		form.setDays(pwdReminderService.getPwdReminderDaysBefore());

		try {
			pwdReminderService.setPwdReminderTime(LocalTime.parse(form.getTime()));
		} catch (Exception ex) {
			log.warn("Bad time: " + form.getTime());

			form.setTime(pwdReminderService.getPwdReminderTime().toString());
		}

		model.addAttribute("allOrgUnits", orgUnitService.getAllTree());
		model.addAttribute("reminderForm", form);

		return "sms/pwdreminders";
	}

	@GetMapping("/ui/sms/pwdlocked")
	public String pwdLocked(Model model) {
		PwdLockedDTO form = new PwdLockedDTO();
		form.setSmsTxt(pwdLockedService.getPwdReminderSmsTxt());
		form.setEnabled(pwdLockedService.getPwdLockedEnabled());

		model.addAttribute("passwordLockedForm", form);

		return "sms/pwdlocked";
	}

	@PostMapping("/ui/sms/pwdlocked")
	public String pwdLocked(Model model, PwdLockedDTO form) {
		pwdLockedService.setPwdReminderSmsTxt(form.getSmsTxt());
		pwdLockedService.setPwdLockedEnabled(form.isEnabled());

		model.addAttribute("passwordLockedForm", form);

		return "sms/pwdlocked";
	}
}
