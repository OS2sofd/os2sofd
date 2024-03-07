package dk.digitalidentity.sofd.controller.mvc;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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

import dk.digitalidentity.sofd.controller.mvc.dto.PhoneNumberDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.PwdLockedDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.PwdReminderDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.RecipientType;
import dk.digitalidentity.sofd.controller.mvc.dto.SearchForm;
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
	public String createMessagePost(Model model, String selectedMessage, HttpServletRequest httpServletRequest) {
		httpServletRequest.getSession().setAttribute("selectedMessage", selectedMessage);

		model.addAttribute("allItSystems", roleCatalogService.fetchAllItSystems());
		model.addAttribute("allOrgUnits", orgUnitService.getAllTree());
		model.addAttribute("allOrgUnitsV2", orgUnitService.getAllTreeByAddress());
		model.addAttribute("searchForm", new SearchForm());

		return "sms/search";
	}

	@PostMapping("/ui/sms/filter")
	public String filter(Model model, SearchForm searchForm, HttpServletRequest httpServletRequest) {
		Set<PhoneNumberDTO> phoneNumbers = selectPhoneNumbers(searchForm);
		
		// make sure they have unique ID's to help the Java
		long i = 1;
		for (PhoneNumberDTO phone : phoneNumbers) {
			phone.setId(i++);
		}

		model.addAttribute("phoneNumbers", phoneNumbers);

		return "sms/filter";
	}

	private Set<PhoneNumberDTO> selectPhoneNumbers(SearchForm searchForm) {
		Set<PhoneNumberDTO> phoneNumbers = new HashSet<PhoneNumberDTO>();
		Set<String> addedPhoneNumbers = new HashSet<>();

		// search by OrgUnit selection
		List<OrgUnit> orgUnits = new ArrayList<OrgUnit>();
		
		if (searchForm.getOrgUnits() != null && searchForm.getOrgUnits().size() > 0) {
			// filter out duplicates
			Set<String> ouUuids = searchForm.getOrgUnits().stream().collect(Collectors.toSet());

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

		List<Person> orgUnitPersons = null;
		if (orgUnits.size() > 0) {
			orgUnitPersons = new ArrayList<Person>();

			for (OrgUnit orgUnit : orgUnits) {
				List<Affiliation> affiliations = affiliationService.findByOrgUnitAndActive(orgUnit);
				for (Affiliation affiliation : affiliations) {
					addPerson(orgUnitPersons, affiliation.getPerson());
				}

				// we only add phone-numbers from OrgUnits if we are not filtering on it-systems and/or functions
				if ((searchForm.getItSystems() == null || searchForm.getItSystems().size() == 0) &&
					(!StringUtils.hasLength(searchForm.getFunction()) || "NONE".equals(searchForm.getFunction()))) {
					phoneNumbers.addAll(getPhones(orgUnit));
				}
			}
		}

		// search by function
		List<Person> functionPersons = null;
		if ("LEDER".equals(searchForm.getFunction())) {
			functionPersons = personService.findAllManagers();
		}
		else if ("TR".equals(searchForm.getFunction())) {
			functionPersons = personService.findAllTRs();
		}
		else if ("SR".equals(searchForm.getFunction())) {
			functionPersons = personService.findAllSRs();
		}

		List<Person> itSystemPersons = null;
		if (searchForm.getItSystems() != null && searchForm.getItSystems().size() > 0) {
			Set<String> userIds = new HashSet<>();

			for (String itSystemIdentifier : searchForm.getItSystems()) {
				Set<String> userIdsFromItSystem = roleCatalogService.getUsersByItSystem(itSystemIdentifier);

				userIds.addAll(userIdsFromItSystem);
			}

			itSystemPersons = personService.getAll().stream()
					.filter(p -> PersonService.getUsers(p).stream().filter(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType())).anyMatch(u -> userIds.contains(u.getUserId())))
					.collect(Collectors.toList());
		}

		if (searchForm.getOperation().equals("OR")) {
			if (functionPersons != null) {
				for (Person person : functionPersons) {
					PhoneNumberDTO phone = getPhone(person);
					if (phone != null) {
						phoneNumbers.add(phone);
						addedPhoneNumbers.add(phone.getPhone());
					}
				}
			}

			if (orgUnitPersons != null) {
				for (Person person : orgUnitPersons) {
					PhoneNumberDTO phone = getPhone(person);
					if (phone != null) {
						phoneNumbers.add(phone);
						addedPhoneNumbers.add(phone.getPhone());
					}
				}
			}

			if (itSystemPersons != null) {
				for (Person person : itSystemPersons) {
					PhoneNumberDTO phone = getPhone(person);
					if (phone != null) {
						phoneNumbers.add(phone);
						addedPhoneNumbers.add(phone.getPhone());
					}
				}
			}

			if (searchForm.isIncludeOrgUnits() && !orgUnits.isEmpty()) {
				for (OrgUnit orgUnit : orgUnits) {
					PhoneNumberDTO phone = getPhone(orgUnit);
					if (phone != null) {
						phoneNumbers.add(phone);
						addedPhoneNumbers.add(phone.getPhone());
					}
				}
			}
		}
		else if (searchForm.getOperation().equals("AND")) {
			List<List<Person>> lists = new ArrayList<>();

			if (functionPersons != null) {
				lists.add(functionPersons);
			}

			if (itSystemPersons != null) {
				lists.add(itSystemPersons);
			}

			if (orgUnitPersons != null) {
				lists.add(orgUnitPersons);
			}

			List<Person> firstList = lists.get(0);
			if (lists.size() > 1) {
				lists.remove(0);

				Iterator<Person> iterator = firstList.iterator();
				while (iterator.hasNext()) {
					Person person = iterator.next();

					for (List<Person> otherList : lists) {
						boolean found = false;

						for (Person otherPerson : otherList) {
							if (otherPerson.getUuid().equals(person.getUuid())) {
								found = true;
								break;
							}
						}

						// one of the other lists did NOT contain this person, so remove it
						if (!found) {
							iterator.remove();
							break;
						}
					}
				}
			}

			for (Person person : firstList) {
				PhoneNumberDTO phone = getPhone(person);
				if (phone != null) {
					phoneNumbers.add(phone);
					addedPhoneNumbers.add(phone.getPhone());
				}
			}

			if (searchForm.isIncludeOrgUnits() && itSystemPersons == null && functionPersons == null && !orgUnits.isEmpty()) {
				for (OrgUnit orgUnit : orgUnits) {
					PhoneNumberDTO phone = getPhone(orgUnit);
					if (phone != null) {
						phoneNumbers.add(phone);
						addedPhoneNumbers.add(phone.getPhone());
					}
				}
			}
		}

		// remove nulls if present
		phoneNumbers.remove(null);

		// remove duplicates
		HashSet<String> seen = new HashSet<>();
		phoneNumbers.removeIf(ph -> !seen.add(ph.getPhone()));

		return phoneNumbers;
	}

	@PostMapping("/ui/sms/sendmessage")
	public ResponseEntity<?> send(@RequestBody List<PhoneNumberDTO> selectedNumbers, HttpServletRequest httpServletRequest) {
		String message = (String) httpServletRequest.getSession().getAttribute("selectedMessage");
		Set<String> numbers = selectedNumbers.stream().filter(a -> a.getName() != null && a.getPhone() != null).map(a -> a.getPhone()).collect(Collectors.toSet());

		SmsLog newLog = new SmsLog();
		newLog.setRecipients(new HashSet<>());
		newLog.setMessage(message);
		newLog.setUserId(SecurityUtil.getUser());

		if (numbers != null && numbers.size() > 0 && message != null && message.length() > 0) {
			smsService.sendMessage(message, numbers);
			for (PhoneNumberDTO phoneNumberDTO : selectedNumbers) {
				if (phoneNumberDTO.getName() == null || phoneNumberDTO.getPhone() == null) {
					continue;
				}
				SmsLogRecipient recipient = new SmsLogRecipient();
				recipient.setName(phoneNumberDTO.getName());
				recipient.setPhone(phoneNumberDTO.getPhone());
				// TODO: fix this
				recipient.setType(phoneNumberDTO.getType() != null ? phoneNumberDTO.getType() : RecipientType.PERSON);
				recipient.setSmsLog(newLog);

				newLog.getRecipients().add(recipient);
			}
			smsLogService.save(newLog);
		}
		else {
			return ResponseEntity.badRequest().body("Ingen modtagere fundet");
		}

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

	private PhoneNumberDTO getPhone(Person person) {
		Optional<Phone> phone = PersonService.getPhones(person).stream().filter(p -> p.getPhoneType().equals(PhoneType.MOBILE) && p.isTypePrime()).findFirst();

		if (phone.isPresent()) {
			String name = PersonService.getName(person);
			return new PhoneNumberDTO(name, phone.get().getPhoneNumber(), RecipientType.PERSON);
		}

		return null;
	}

	private PhoneNumberDTO getPhone(OrgUnit orgUnit) {
		Optional<Phone> phone = OrgUnitService.getPhones(orgUnit).stream().filter(p -> p.getPhoneType().equals(PhoneType.MOBILE) && p.isTypePrime()).findFirst();

		if (phone.isPresent()) {
			String name = orgUnit.getName();
			return new PhoneNumberDTO(name, phone.get().getPhoneNumber(), RecipientType.ORGUNIT);
		}

		return null;
	}

	private List<PhoneNumberDTO> getPhones(OrgUnit orgUnit) {
		List<PhoneNumberDTO> result = new ArrayList<>();

		if (orgUnit.getPhones() != null) {
			for (Phone phone : OrgUnitService.getPhones(orgUnit)) {
				// no TypePrime check here, get them all
				if (phone.getPhoneType().equals(PhoneType.MOBILE)) {
					result.add(new PhoneNumberDTO(orgUnit.getName(), phone.getPhoneNumber(), RecipientType.ORGUNIT));
				}
			}
		}

		return result;
	}

	private void addPerson(List<Person> persons, Person person) {
		if (PersonService.getPhones(person).stream().anyMatch(p -> p.getPhoneType().equals(PhoneType.MOBILE) && p.isTypePrime())) {
			persons.add(person);
		}
	}
}
