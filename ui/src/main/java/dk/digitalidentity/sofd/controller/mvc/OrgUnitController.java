package dk.digitalidentity.sofd.controller.mvc;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import dk.digitalidentity.sofd.config.RoleConstants;
import dk.digitalidentity.sofd.controller.mvc.admin.dto.TagDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.AffiliationDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.EmployeeDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.EmployeeWithUsersDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.KleDTO;
import dk.digitalidentity.sofd.controller.mvc.xls.AccountOrderRulesXlsDto;
import dk.digitalidentity.sofd.controller.mvc.xls.AccountOrderRulesXlsView;
import dk.digitalidentity.sofd.controller.mvc.xls.EmployeesInformationXlsView;
import dk.digitalidentity.sofd.controller.mvc.xls.OrgUnitsXlsxView;
import dk.digitalidentity.sofd.controller.rest.model.PhoneDTO;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.OrgUnitAccountOrder;
import dk.digitalidentity.sofd.dao.model.OrgUnitTag;
import dk.digitalidentity.sofd.dao.model.OrgUnitType;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SubstituteContext;
import dk.digitalidentity.sofd.dao.model.SupportedUserType;
import dk.digitalidentity.sofd.dao.model.Tag;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderRule;
import dk.digitalidentity.sofd.dao.model.enums.AffiliationType;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.EventType;
import dk.digitalidentity.sofd.log.AuditLogger;
import dk.digitalidentity.sofd.security.RequireControllerWriteAccess;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.AccountOrderService;
import dk.digitalidentity.sofd.service.AffiliationService;
import dk.digitalidentity.sofd.service.FunctionTypeService;
import dk.digitalidentity.sofd.service.KleService;
import dk.digitalidentity.sofd.service.OrgUnitFutureChangesService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.OrganisationService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SubstituteContextService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import dk.digitalidentity.sofd.service.TagsService;
import dk.digitalidentity.sofd.service.model.KleAssignmentDto;
import dk.digitalidentity.sofd.service.model.KleAssignmentType;
import dk.digitalidentity.sofd.service.model.OUTreeForm;
import dk.digitalidentity.sofd.service.model.OUTreeFormWithTags;
import dk.digitalidentity.sofd.service.model.SubstituteOrgUnitAssignmentDTO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequireReadAccess
@Controller
public class OrgUnitController {

	@Autowired
	private OrgUnitService orgUnitService;

	@Autowired
	private KleService kleService;

	@Autowired
	private AffiliationService affiliationService;

	@Autowired
	private FunctionTypeService functionTypeService;

	@Autowired
	private AccountOrderService accountOrderService;

	@Autowired
	private SupportedUserTypeService supportedUserTypeService;

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private OrgUnitFutureChangesService orgUnitFutureChangesService;

	@Autowired
	private TagsService tagsService;

	@Autowired
	private PersonService personService;

	@Autowired
	private AuditLogger auditLogger;

	@Autowired
	private OrganisationService organisationService;

	@Autowired
	private SubstituteContextService substituteContextService;
    @Autowired
    private SofdConfiguration sofdConfiguration;

	record TagAssignmentDTO(String uuid, String value, String shortValue) {}
	@GetMapping("/ui/orgunit")
	public String list(Model model) {
		List<OUTreeFormWithTags> allTreeWithTags = orgUnitService.getAllTreeWithTags();
		model.addAttribute("orgUnits", allTreeWithTags);
		model.addAttribute("tags", tagsService.findAll());

		Map<Long, List<TagAssignmentDTO>> tags = new HashMap<>();
		for (OUTreeFormWithTags ou : allTreeWithTags) {
			for (Long tagId : ou.getTagIds()) {
				if (!tags.containsKey(tagId)) {
					tags.put(tagId, new ArrayList<>());
					String value = ou.getTagValueMap().get(tagId);
					tags.get(tagId).add(new TagAssignmentDTO(ou.getId(), value, getShortValue(value)));
				}
				else {
					String value = ou.getTagValueMap().get(tagId);
					tags.get(tagId).add(new TagAssignmentDTO(ou.getId(), value, getShortValue(value)));
				}
			}
		}

		model.addAttribute("tagsmap", tags);

		return "orgunit/tree";
	}

	private String getShortValue(String value) {
		if (value != null) {
			if (value.length() > 20) {
				return value.substring(0, 20) + "...";
			} else {
				return value;
			}
		}
		return "";
	}

	@GetMapping("/ui/orgunit/downloadrules")
	public ModelAndView downloadRules(HttpServletResponse response, Locale loc) {
		AccountOrderRulesXlsDto dto = new AccountOrderRulesXlsDto();

		List<OrgUnit> orgUnits = orgUnitService.getAllActive();
		for (OrgUnit orgUnit : orgUnits) {
			dto.add(orgUnit, accountOrderService.getAccountOrderSettings(orgUnit, true));
		}

		Map<String, Object> model = new HashMap<>();
		model.put("employees", dto);
		model.put("supportedUserTypeService", supportedUserTypeService);
		model.put("messagesBundle", messageSource);
		model.put("locale", loc);

		response.setContentType("application/ms-excel");
		response.setHeader("Content-Disposition", "attachment; filename=\"regler.xlsx\"");

		return new ModelAndView(new AccountOrderRulesXlsView(), model);
	}

	@GetMapping(path = {"/ui/orgunit/downloadorgunits/{orgId}"})
	public ModelAndView downloadOrgUnits(HttpServletResponse response, Locale loc, @PathVariable("orgId") Long orgId) {
		List<OrgUnit> orgUnits = (orgId == null || organisationService.getById(orgId) == null)
				? orgUnitService.getAllActive()
				: orgUnitService.getAllActive(organisationService.getById(orgId));

		Map<String, Object> model = new HashMap<>();
		model.put("orgUnits", orgUnits);
		model.put("messagesBundle", messageSource);
		model.put("locale", loc);

		response.setContentType("application/ms-excel");
		response.setHeader("Content-Disposition", "attachment; filename=\"enheder.xlsx\"");

		return new ModelAndView(new OrgUnitsXlsxView(), model);
	}

	@GetMapping(path = {"/ui/orgunit/view/{uuid}"})
	public String view(Model model, @PathVariable("uuid") String uuid) throws Exception {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			log.warn("No OrgUnit with uuid " + uuid);
			return "redirect:/ui/orgunit";
		}

		List<KleDTO> kleDTOs = kleService.findAll().stream().map(k -> new KleDTO(k)).sorted(Comparator.comparing(KleDTO::getId)).collect(Collectors.toList());
		List<TagDTO> tags = new ArrayList<>();
		for (Tag tag : tagsService.findAll()) {
			Optional<OrgUnitTag> selectedTag = orgUnit.getTags().stream().filter(t -> t.getTag().equals(tag)).findFirst();
			TagDTO tagDTO = new TagDTO(tag);

			if (selectedTag.isPresent()) {
				tagDTO.setSelected(true);
				tagDTO.setCustomValue(selectedTag.get().getCustomValue());
			}

			tags.add(tagDTO);
		}
		model.addAttribute("allKles", kleDTOs);
		model.addAttribute("tags", tags);
		model.addAttribute("orgUnit", orgUnit);
		model.addAttribute("inheritedEan", "1234567890");
		model.addAttribute("postAddresses", OrgUnitService.getPosts(orgUnit));
		model.addAttribute("phones", OrgUnitService.getPhones(orgUnit).stream().map(p -> new PhoneDTO(p)).collect(Collectors.toList()));
		model.addAttribute("employees", getEmployees(orgUnit));
		model.addAttribute("functionTypes", functionTypeService.findAllAsDTO());
		model.addAttribute("futureChanges", orgUnitFutureChangesService.getAllByOrgUnitAndNotApplied(orgUnit));
		model.addAttribute("substituteContexts", substituteContextService.getAll().stream().filter(SubstituteContext::isAssignableToOrgUnit).toList());

		List<SubstituteOrgUnitAssignmentDTO> substitutes = orgUnitService.getOrgUnitSubstitutes(orgUnit);
		model.addAttribute("substitutes", substitutes);

		// TODO: these we only need to load for admins when the account creation module is enabled....
		OrgUnitAccountOrder accountOrders = accountOrderService.getAccountOrderSettings(orgUnit, true);
		model.addAttribute("orgUnitAccountOrder", accountOrders);
		model.addAttribute("ous", orgUnitService.getAllTree());
		model.addAttribute("canCopyRules", !accountOrders.getTypes().stream().anyMatch(t -> t.getRule().equals(AccountOrderRule.BY_POSITION_NAME)));

		return "orgunit/view";
	}

	@GetMapping(value = "/ui/orgunit/view/download/employees/{uuid}")
	public ModelAndView downloadEmployeesInformation(HttpServletResponse response, Locale loc, @PathVariable("uuid") String uuid) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			log.warn("No OrgUnit with uuid " + uuid);
			return new ModelAndView("redirect:/ui/orgunit");
		}

		Map<String, Object> model = new HashMap<>();
		model.put("employees", getEmployeesWithUsers(orgUnit));
		model.put("supportedUserTypeService", supportedUserTypeService);
		model.put("messagesBundle", messageSource);
		model.put("locale", loc);

		response.setContentType("application/ms-excel");
		response.setHeader("Content-Disposition", "attachment; filename=\"Medarbejderoplysninger - " + orgUnit.getName() + ".xlsx\"");

		return new ModelAndView(new EmployeesInformationXlsView(), model);
	}

	@GetMapping(value = "/ui/orgunit/view/download/employees_nested/{uuid}")
	public ModelAndView downloadEmployeesInformationNested(HttpServletResponse response, Locale loc, @PathVariable("uuid") String uuid) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			log.warn("No OrgUnit with uuid " + uuid);
			return new ModelAndView("redirect:/ui/orgunit");
		}

		Map<String, Object> model = new HashMap<>();
		model.put("employees", getEmployeesWithUsersRecursive(orgUnit));
		model.put("supportedUserTypeService", supportedUserTypeService);
		model.put("messagesBundle", messageSource);
		model.put("locale", loc);

		response.setContentType("application/ms-excel");
		response.setHeader("Content-Disposition", "attachment; filename=\"Medarbejderoplysninger - " + orgUnit.getName() + ".xlsx\"");

		return new ModelAndView(new EmployeesInformationXlsView(), model);
	}

	@RequireControllerWriteAccess
	@GetMapping("/ui/orgunit/view/{uuid}/addemployee")
	public String addEmployee(Model model, @PathVariable("uuid") String uuid) throws Exception {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			log.warn("No OrgUnit with uuid " + uuid);
			return "redirect:/ui/orgunit";
		}

		model.addAttribute("orgUnit", orgUnit);

		return "orgunit/add_employee";
	}

	@RequireControllerWriteAccess
	@GetMapping("/ui/orgunit/affiliation/{orgUnitUuid}/{personUuid}")
	public String newAffiliation(Model model, @PathVariable("orgUnitUuid") String orgUnitUuid, @PathVariable("personUuid") String personUuid) {
		OrgUnit orgUnit = orgUnitService.getByUuid(orgUnitUuid);
		if (orgUnit == null) {
			log.warn("No OrgUnit with uuid " + orgUnitUuid);
			return "redirect:/ui/orgunit";
		}

		AffiliationDTO affiliationDTO = new AffiliationDTO();
		affiliationDTO.setAffiliationType(AffiliationType.EMPLOYEE);

		model.addAttribute("affiliationDTO", affiliationDTO);
		model.addAttribute("personUUID", personUuid);
		model.addAttribute("orgUnit", orgUnit);

		return "orgunit/new_affiliation";
	}

	@RequireControllerWriteAccess
	@PostMapping("/ui/orgunit/affiliation")
	public String createNewAffiliation(Model model, @ModelAttribute("personUUID") String personUUID, @Valid @ModelAttribute("affiliationDTO") AffiliationDTO affiliationDTO, BindingResult bindingResult) {
		OrgUnit ou = orgUnitService.getByUuid(affiliationDTO.getOrgUnitUuid());
		if (ou == null) {
			log.warn("Could not find orgUnit with uuid " + affiliationDTO.getOrgUnitUuid() + " while assigning new affiliation");

			return "redirect:/ui/orgunit";
		}

		if (bindingResult.hasErrors()) {
			model.addAttribute(bindingResult.getAllErrors());
			model.addAttribute("affiliationDTO", affiliationDTO);
			model.addAttribute("personUUID", personUUID);
			model.addAttribute("orgUnit", ou);

			return "orgunit/new_affiliation";
		}

		Person person = personService.getByUuid(personUUID);
		if (person == null) {
			log.warn("Could not find person with uuid " + personUUID + " while assigning new affiliation");

			return "redirect:/ui/orgunit/view/" + affiliationDTO.getOrgUnitUuid() + "/addemployee";
		}

		addAffiliationFromDTO(affiliationDTO, person, ou);

		personService.save(person);

		var logMessage = new StringBuilder();
		logMessage.append("Tilhørsforhold oprettet via OS2sofd GUI. ");
		logMessage.append(person.getEntityLogInfo()).append(", ");
		logMessage.append("Startdato: ").append(affiliationDTO.getStartDate()).append(", ");
		logMessage.append("Slutdato: ").append(affiliationDTO.getStopDate()).append(", ");
		logMessage.append("Stillingsbetegnelse: ").append(affiliationDTO.getPositionName()).append(", ");
		logMessage.append("Enhed: ").append(ou.getName());

		auditLogger.log(person.getUuid(), EntityType.PERSON, EventType.AFFILIATION_CREATED, PersonService.getName(person), logMessage.toString());

		return "redirect:/ui/orgunit/view/" + affiliationDTO.getOrgUnitUuid();
	}

	@GetMapping("/ui/orgunit/view/{uuid}/addemployees")
	public String addEmployees(Model model, @PathVariable("uuid") String uuid) throws Exception {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			log.warn("No OrgUnit with uuid " + uuid);

			return "redirect:/ui/orgunit";
		}

		model.addAttribute("orgUnits", orgUnitService.getAllTree());
		model.addAttribute("orgUnit", orgUnit);

		return "orgunit/add_employees";
	}

	@RequireControllerWriteAccess
	@GetMapping("/ui/orgunit/affiliations/{toOrgUnitUuid}/{fromOrgUnitUuid}")
	public String newAffiliations(Model model, @PathVariable("toOrgUnitUuid") String toOrgUnitUuid, @PathVariable("fromOrgUnitUuid") String fromOrgUnitUuid) {
		AffiliationDTO affiliationDTO = new AffiliationDTO();
		affiliationDTO.setAffiliationType(AffiliationType.EMPLOYEE);

		OrgUnit toOrgUnit = orgUnitService.getByUuid(toOrgUnitUuid);
		if (toOrgUnit == null) {
			log.warn("No OrgUnit with uuid " + toOrgUnitUuid);
			return "redirect:/ui/orgunit";
		}

		OrgUnit fromOrgUnit = orgUnitService.getByUuid(fromOrgUnitUuid);
		if (fromOrgUnit == null) {
			log.warn("No OrgUnit with uuid " + fromOrgUnitUuid);
			return "redirect:/ui/orgunit";
		}

		model.addAttribute("affiliationDTO", affiliationDTO);
		model.addAttribute("toOrgUnit", toOrgUnit);
		model.addAttribute("fromOrgUnit", fromOrgUnit);

		return "orgunit/new_affiliations";
	}

	@RequireControllerWriteAccess
	@PostMapping("/ui/orgunit/affiliations")
	public String createNewAffiliations(Model model, @Valid @ModelAttribute("affiliationDTO") AffiliationDTO affiliationDTO, BindingResult bindingResult) {
		OrgUnit ou = orgUnitService.getByUuid(affiliationDTO.getOrgUnitUuid());
		if (ou == null) {
			log.warn("Could not find orgUnit with uuid " + affiliationDTO.getOrgUnitUuid() + " while assigning new affiliation");
			return "/ui/orgunit";
		}

		OrgUnit fromOu = orgUnitService.getByUuid(affiliationDTO.getFromOrgUnitUuid());
		if (fromOu == null) {
			log.warn("Could not find orgUnit with uuid " + affiliationDTO.getFromOrgUnitUuid() + " while assigning new affiliation");
			return "/ui/orgunit";
		}

		if (bindingResult.hasErrors()) {
			model.addAttribute(bindingResult.getAllErrors());
			model.addAttribute("affiliationDTO", affiliationDTO);
			model.addAttribute("ous", orgUnitService.getAllTree());
			model.addAttribute("fromOrgUnit", fromOu);
			model.addAttribute("toOrgUnit", ou);

			return "orgunit/new_affiliations";
		}

		for (EmployeeDTO person : getEmployees(fromOu)) {
			Person realPerson = personService.getByUuid(person.getUuid());
			if (realPerson == null) {
				log.warn("Could not find person with uuid " + person.getUuid() + " while assigning new affiliation");
				continue;
			}

			addAffiliationFromDTO(affiliationDTO, realPerson, ou);
			personService.save(realPerson);

			var sb = new StringBuilder();
			sb.append("Tilhørsforhold oprettet via OS2sofd GUI. ");
			sb.append(realPerson.getEntityLogInfo()).append(", ");
			sb.append("Startdato: ").append(affiliationDTO.getStartDate()).append(", ");
			sb.append("Slutdato: ").append(affiliationDTO.getStopDate()).append(", ");
			sb.append("Stillingsbetegnelse: ").append(affiliationDTO.getPositionName()).append(", ");
			sb.append("Enhed: ").append(ou.getName());

			auditLogger.log(person.getUuid(), EntityType.PERSON, EventType.AFFILIATION_CREATED, PersonService.getName(realPerson), sb.toString());
		}

		return "redirect:/ui/orgunit/view/" + affiliationDTO.getOrgUnitUuid();
	}

	@GetMapping("/ui/orgunit/new/{orgId}")
	public String newOU(Model model, @PathVariable("orgId") String orgId) {
		model.addAttribute("organisationId", orgId);
		model.addAttribute("orgUnit", new OrgUnit());
		return "orgunit/new";
	}

	@GetMapping(path = {"/ui/orgunit/description/{uuid}"})
	public String detail(Model model, @PathVariable("uuid") String uuid) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			log.warn("No OrgUnit with uuid " + uuid);
			return "redirect:/ui/orgunit";
		}

		model.addAttribute("orgUnit", orgUnit);
		model.addAttribute("employeeCount", affiliationService.countByOrgUnitAndActive(orgUnit));
		model.addAttribute("employeeCountRecursive", affiliationService.countByOrgUnitAndActiveRecursive(orgUnit));

		return "orgunit/description";
	}

	@GetMapping("/ui/orgunit/viewKLE/{uuid}/{type}")
	public String getViewKLEFragment(Model model, @PathVariable("uuid") String uuid, @PathVariable("type") String type) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			log.warn("No OrgUnit with uuid " + uuid);
			return "fragments/viewKLE :: content";
		}

		List<KleAssignmentDto> kles = new ArrayList<>();

		switch (type) {
			case "KlePrimary":
				kles = orgUnitService.getKleAssignments(orgUnit, KleAssignmentType.PRIMARY);
				break;
			case "KleSecondary":
				kles = orgUnitService.getKleAssignments(orgUnit, KleAssignmentType.SECONDARY);
				break;
			case "KleTertiary":
				kles = orgUnitService.getKleAssignments(orgUnit, KleAssignmentType.TERTIARY);
				break;
			default:
				log.error("Invalid kle type: " + type);
				break;
		}

		model.addAttribute("kles", kles);

		return "fragments/viewKLE :: content";
	}

	@GetMapping("/ui/orgunit/editKLE/{uuid}/{type}")
	public String getEditKLEFragment(Model model, @PathVariable("uuid") String uuid, @PathVariable("type") String type) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			log.warn("No OrgUnit with uuid " + uuid);
			return "fragments/editKLE :: content";
		}

		List<String> kleSelected = new ArrayList<String>();
		switch (type) {
			case "KlePrimary":
				kleSelected = orgUnit.getKlePrimary().stream().map(k -> k.getKleValue()).collect(Collectors.toList());
				break;
			case "KleSecondary":
				kleSelected = orgUnit.getKleSecondary().stream().map(k -> k.getKleValue()).collect(Collectors.toList());
				break;
			case "KleTertiary":
				kleSelected = orgUnit.getKleTertiary().stream().map(k -> k.getKleValue()).collect(Collectors.toList());
				break;
			default:
				log.error("Invalid kle type: " + type);
				break;
		}

		model.addAttribute("type", type);
		model.addAttribute("kleSelected", kleSelected);

		return "fragments/editKLE :: content";
	}

	private List<EmployeeDTO> getEmployees(OrgUnit orgUnit) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		List<EmployeeDTO> employees = new ArrayList<>();

		for (Affiliation affiliation : affiliationService.findByCalculatedOrgUnitAndActive(orgUnit)) {
			EmployeeDTO employeeDTO = new EmployeeDTO();
			Person person = affiliation.getPerson();

			employeeDTO.setName(PersonService.getName(person));
			employeeDTO.setUuid(person.getUuid());
			employeeDTO.setPositionName(AffiliationService.getPositionName(affiliation));
			employeeDTO.setEmploymentTermsText(affiliation.getEmploymentTermsText());
			employeeDTO.setAccounts(new ArrayList<>());
			employeeDTO.setVendor(affiliation.getVendor());
			employeeDTO.setStartDate(affiliation.getStartDate() != null ? formatter.format(affiliation.getStartDate()) : null);
			employeeDTO.setStopDate(affiliation.getStopDate() != null ? formatter.format(affiliation.getStopDate()) : null);
			employeeDTO.setLeave(person.getLeave() != null);
			employeeDTO.setForceStop(person.isForceStop());
			employeeDTO.setDisableAccountOrdersCreate(person.isDisableAccountOrdersCreate());
			employeeDTO.setDisableAccountOrdersDisable(person.isDisableAccountOrdersDisable());
			employeeDTO.setDisableAccountOrdersDelete(person.isDisableAccountOrdersDelete());
			employeeDTO.setFictiveCpr(person.isFictiveCpr());

			for (User user : PersonService.getUsers(person)) {
				SupportedUserType userType = supportedUserTypeService.findByKey(user.getUserType());
				if (userType == null) {
					log.error("person " + person.getUuid() + " has user account with unknown type " + user.getUserType());
					continue;
				}

				employeeDTO.getAccounts().add(Pair.of(user.getUserId() + " (" + userType.getName() + ")", user.isDisabled()));
			}

			employees.add(employeeDTO);
		}

		return employees;
	}

	private List<EmployeeWithUsersDTO> getEmployeesWithUsers(OrgUnit orgUnit) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		List<EmployeeWithUsersDTO> employees = new ArrayList<>();

		for (Affiliation affiliation : affiliationService.findByCalculatedOrgUnitAndActive(orgUnit)) {
			EmployeeWithUsersDTO employeeDTO = new EmployeeWithUsersDTO();
			Person person = affiliation.getPerson();

			employeeDTO.setName(PersonService.getName(person));
			employeeDTO.setUuid(person.getUuid());
			employeeDTO.setPositionName(AffiliationService.getPositionName(affiliation));
			employeeDTO.setUsers(new ArrayList<>());
			employeeDTO.setStartDate(affiliation.getStartDate() != null ? formatter.format(affiliation.getStartDate()) : null);
			employeeDTO.setStopDate(affiliation.getStopDate() != null ? formatter.format(affiliation.getStopDate()) : null);
			employeeDTO.setOrgUnitName(orgUnit.getName());
			var manager = PersonService.getManagerDifferentFromPerson(person, affiliation);
			if( manager != null ) {
				employeeDTO.setManager(PersonService.getName(manager.getManager()));
				employeeDTO.setManagerUsername(manager.getManager().getPrimeADAccount());
				employeeDTO.setManagerEmail(manager.getManager().getPrimeEmail());
				var managerPrimeAffiliation = manager.getManager().getPrimeAffiliation();
				if( managerPrimeAffiliation != null ) {
					employeeDTO.setManagerEmployeeNumber(managerPrimeAffiliation.isFromWageSystem() ? managerPrimeAffiliation.getEmployeeId() : null);
				}
			}
			employeeDTO.setOrgUnitUuid(orgUnit.getUuid());
			employeeDTO.setEmploymentTerms(affiliation.getEmploymentTermsText());
			employeeDTO.setInternalReference(affiliation.getInternalReference());
			employeeDTO.setPrimeAffiliation(affiliation.isPrime());
			employeeDTO.setEmployeeNumber(affiliation.isFromWageSystem() ? affiliation.getEmployeeId() : null);
			employeeDTO.setOnLeave(person.isOnActiveLeave());
			employeeDTO.setInheritPrivileges(affiliation.isInheritPrivileges());

			for (User user : PersonService.getUsers(person)) {
				SupportedUserType userType = supportedUserTypeService.findByKey(user.getUserType());
				if (userType == null) {
					log.error("person " + person.getUuid() + " has user account with unknown type " + user.getUserType());
					continue;
				}
				if( SupportedUserTypeService.isActiveDirectory(user.getUserType())) {
					// only add user if mapped to this affiliation or if not mapped at all.
					if(user.getEmployeeId() == null || user.getEmployeeId().equalsIgnoreCase(affiliation.getEmployeeId()))
					{
						employeeDTO.getUsers().add(user);
						// also add any exchange account that has this user as master
						var exchangeUser = PersonService.getUsers(person).stream().filter(u -> SupportedUserTypeService.isExchange(u.getUserType()) && u.getMasterId().equalsIgnoreCase(user.getUserId())).findFirst().orElse(null);
						if( exchangeUser != null ) {
							employeeDTO.getUsers().add(exchangeUser);
						}
					}
				}
				else if (SupportedUserTypeService.isExchange(user.getUserType())) {
					// ignore. This case is handled in the active directory logic above
				}
				else
				{
					employeeDTO.getUsers().add(user);
				}
			}

			employees.add(employeeDTO);
		}

		return employees;
	}

	private List<EmployeeWithUsersDTO> getEmployeesWithUsersRecursive(OrgUnit ou) {
		List<EmployeeWithUsersDTO> allEmployees = new ArrayList<>();

		allEmployees.addAll(getEmployeesWithUsers(ou));
		for (OrgUnit childOU : ou.getChildren()) {
			allEmployees.addAll(getEmployeesWithUsersRecursive(childOU));
		}

		return allEmployees;
	}

	@GetMapping("/ui/orgunit/phoneTab/{uuid}")
	public String getPhoneTab(Model model, @PathVariable("uuid") String uuid) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			log.warn("No OrgUnit with uuid: " + uuid);
			return "fragments/phoneTab :: phoneTab";
		}

		model.addAttribute("phones", OrgUnitService.getPhones(orgUnit).stream().map(p -> new PhoneDTO(p)).collect(Collectors.toList()));

		return "fragments/phoneTab :: phoneTab";
	}

	@GetMapping("/ui/orgunit/phonePrime/{uuid}")
	public String getPhonePrime(Model model, @PathVariable("uuid") String uuid) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			log.warn("No OrgUnit with uuid: " + uuid);
			return "fragments/phoneTab :: phonePrimeModal";
		}

		model.addAttribute("phones", OrgUnitService.getPhones(orgUnit).stream().map(p -> new PhoneDTO(p)).collect(Collectors.toList()));

		return "fragments/phoneTab :: phonePrimeModal";
	}

	@GetMapping("/ui/orgunit/contactInfo/{uuid}/{type}")
	public String getContactInfoTab(Model model, @PathVariable("uuid") String uuid,  @PathVariable("type") String type) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			log.warn("No OrgUnit with uuid: " + uuid);
		}
		else {
			model.addAttribute("contactAddress", orgUnit.getContactAddress());
			model.addAttribute("openingHours", orgUnit.getOpeningHours());
			model.addAttribute("openingHoursPhone", orgUnit.getOpeningHoursPhone());
			model.addAttribute("keyWords", orgUnit.getKeyWords());
			model.addAttribute("notes", orgUnit.getNotes());
			model.addAttribute("emailNotes", orgUnit.getEmailNotes());
			model.addAttribute("location", orgUnit.getLocation());
			model.addAttribute("urlAddress", orgUnit.getUrlAddress());
			model.addAttribute("email", orgUnit.getEmail());
		}

		if (type.equals("edit")) {
			return "orgunit/fragments/editContactInfoTab :: contactInfoTab";
		}

		return "orgunit/fragments/viewContactInfoTab :: contactInfoTab";
	}

	@GetMapping("/ui/orgunit/orderAccounts/{uuid}/{type}")
	public String getOrderAccountsTab(Model model, @PathVariable("uuid") String uuid, @PathVariable("type") String type) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			log.warn("No OrgUnit with uuid: " + uuid);
		}
		else {
			OrgUnitAccountOrder accountOrders = accountOrderService.getAccountOrderSettings(orgUnit, true);
			model.addAttribute("orgUnitAccountOrder", accountOrders);
			model.addAttribute("ous", orgUnitService.getAllTree());
			model.addAttribute("canCopyRules", !accountOrders.getTypes().stream().anyMatch(t -> t.getRule().equals(AccountOrderRule.BY_POSITION_NAME)));
		}

		if (type.equals("edit")) {
			return "orgunit/fragments/editAccountOrdersTab :: accountOrdersTab";
		}

		return "orgunit/fragments/viewAccountOrdersTab :: accountOrdersTab";
	}

	@GetMapping("/ui/orgunit/core/{uuid}/{type}")
	public String getCoreFragment(Model model, @PathVariable("uuid") String uuid, @PathVariable("type") String type) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			log.warn("No OrgUnit with uuid: " + uuid);
			orgUnit = new OrgUnit();
		}

		model.addAttribute("orgUnit", orgUnit);
		model.addAttribute("doNotTransferInherited", !orgUnit.isDoNotTransferToFkOrg() && orgUnitService.getDoNotTransferToFKOrgUuids().contains(orgUnit.getUuid()));

		if (type.equals("edit")) {
			// filter children of selected ou to prevent recursive reference
			List<OUTreeForm> orgUnits = orgUnitService.getOuTreeWithoutChildren(orgUnit);
			List<OrgUnitType> orgUnitTypes = orgUnitService.getTypes();

			boolean canEditAll = SecurityUtil.getUserRoles().contains(RoleConstants.USER_ROLE_LOS_ADMIN) && "SOFD".equals(orgUnit.getMaster());
			boolean canEditPnr = SecurityUtil.getUserRoles().contains(RoleConstants.USER_ROLE_LOS_ADMIN) && sofdConfiguration.getModules().getLos().isEditPnrOnAllOrgUnitsEnabled();

			model.addAttribute("canEditAll", canEditAll);
			model.addAttribute("canEditPnr", canEditPnr);
			model.addAttribute("orgUnits", orgUnits);
			model.addAttribute("orgUnitTypes", orgUnitTypes);

			return "orgunit/fragments/orgunits_core_edit :: orgUnitsCoreEdit";
		}

		model.addAttribute("deletable", orgUnitService.isDeletable(orgUnit));

		return "orgunit/fragments/orgunits_core_view :: orgUnitsCoreView";
	}

	@GetMapping("/ui/orgunit/postsTab/{uuid}")
	public String getPostsTab(Model model, @PathVariable("uuid") String uuid) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			log.warn("No OrgUnit with uuid: " + uuid);

			model.addAttribute("postAddresses", new ArrayList<>());
		}
		else {
			model.addAttribute("postAddresses", OrgUnitService.getPosts(orgUnit));
		}

		return "orgunit/fragments/viewPostsTab :: postsTab";
	}

	@GetMapping("/ui/orgunit/{uuid}/fragments/managedtitles")
	public String getExtraTitlesFragment(Model model, @PathVariable("uuid") String uuid) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			log.warn("No OrgUnit with uuid " + uuid);
			return "orgunit/fragments/managedTitlesTab :: managedTitlesTab";
		}

		model.addAttribute("orgUnit", orgUnit);

		return "orgunit/fragments/managedTitlesTab :: managedTitlesTab";
	}

	@GetMapping("/ui/orgunit/{uuid}/fragments/ean")
	public String getEanFragment(Model model, @PathVariable("uuid") String uuid) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			log.warn("No OrgUnit with uuid " + uuid);
			return "orgunit/fragments/eanTab :: eanTab";
		}

		model.addAttribute("orgUnit", orgUnit);

		return "orgunit/fragments/eanTab :: eanTab";
	}

	@GetMapping("/ui/orgunit/{uuid}/fragments/ean-create-modal")
	public String getEanCreateModal(Model model, @PathVariable("uuid") String uuid) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			log.warn("No OrgUnit with uuid " + uuid);
			return "orgunit/fragments/eanTab :: eanCreateModal";
		}

		model.addAttribute("orgUnit", orgUnit);

		return "orgunit/fragments/eanTab :: eanCreateModal";
	}

	@GetMapping("/ui/orgunit/{uuid}/fragments/ean-prime-modal")
	public String getEanPrimeModal(Model model, @PathVariable("uuid") String uuid) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			log.warn("No OrgUnit with uuid " + uuid);
			return "orgunit/fragments/eanTab :: eanPrimeModal";
		}

		model.addAttribute("orgUnit", orgUnit);

		return "orgunit/fragments/eanTab :: eanPrimeModal";
	}

	private Affiliation addAffiliationFromDTO(AffiliationDTO affiliationDTO, Person person, OrgUnit ou) {
		Affiliation affiliation = new Affiliation();
		affiliation.setUuid(UUID.randomUUID().toString());
		affiliation.setMaster("SOFD");
		affiliation.setMasterId(UUID.randomUUID().toString());
		affiliation.setOrgUnit(ou);
		affiliation.setPerson(person);
		affiliation.setStartDate(getToday());
		affiliation.setPositionName((StringUtils.hasLength(affiliationDTO.getPositionName())) ? affiliationDTO.getPositionName().trim() : "Ukendt");
		
		affiliation.setAffiliationType(affiliationDTO.getAffiliationType());

		if (affiliationDTO.getAffiliationType().equals(AffiliationType.EXTERNAL)) {
			affiliation.setInheritPrivileges(affiliationDTO.isInheritPrivilegesFromOU());
		}

		if (StringUtils.hasLength(affiliationDTO.getStopDate())) {
			SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

			try {
				Date stopDate = formatter.parse(affiliationDTO.getStopDate());
				affiliation.setStopDate(stopDate);
			}
			catch (ParseException ex) {
				log.warn("Failed to parse: " + affiliationDTO.getStopDate());
			}
		}

		if (person.getAffiliations() == null) {
			person.setAffiliations(new ArrayList<>());
		}
		person.getAffiliations().add(affiliation);

		return affiliation;
	}

	private static Date getToday() {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.HOUR_OF_DAY, 0);

		return cal.getTime();
	}
}
