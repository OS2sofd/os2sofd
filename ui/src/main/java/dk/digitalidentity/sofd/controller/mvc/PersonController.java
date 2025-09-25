package dk.digitalidentity.sofd.controller.mvc;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.history.Revision;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import dk.digitalidentity.sofd.config.RoleConstants;
import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.dto.AffiliationDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.AuthorizationCodeDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.CommentDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.CreatePersonDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.ExchangeAndADDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.FlaggedUserType;
import dk.digitalidentity.sofd.controller.mvc.dto.LocalExtensionDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.PersonDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.PostDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.PrimeUserDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.SimpleAffiliationDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.SubstituteAssignmentDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.SupportedUserTypeSlimDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.UserChangeEmployeeIdQueueDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.UserDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.enums.LocalExtensionType;
import dk.digitalidentity.sofd.controller.rest.model.PhoneDTO;
import dk.digitalidentity.sofd.controller.validation.AffiliationDTOValidator;
import dk.digitalidentity.sofd.controller.validation.CreatePersonDTOValidator;
import dk.digitalidentity.sofd.dao.model.AccountOrder;
import dk.digitalidentity.sofd.dao.model.ActiveDirectoryDetails;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.AuthorizationCode;
import dk.digitalidentity.sofd.dao.model.Notification;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.Post;
import dk.digitalidentity.sofd.dao.model.RevisionId;
import dk.digitalidentity.sofd.dao.model.SupportedUserType;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.UserChangeEmployeeIdQueue;
import dk.digitalidentity.sofd.dao.model.enums.AffiliationType;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.EventType;
import dk.digitalidentity.sofd.dao.model.enums.NotificationType;
import dk.digitalidentity.sofd.dao.model.mapping.OrgUnitPostMapping;
import dk.digitalidentity.sofd.dao.model.mapping.PersonAuthorizationCodeMapping;
import dk.digitalidentity.sofd.log.AuditLogger;
import dk.digitalidentity.sofd.security.RequirePersonCreaterOrControllerWriteAccess;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.AccountOrderService;
import dk.digitalidentity.sofd.service.AffiliationService;
import dk.digitalidentity.sofd.service.CommentService;
import dk.digitalidentity.sofd.service.FunctionTypeService;
import dk.digitalidentity.sofd.service.NotificationService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SubstituteContextService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import dk.digitalidentity.sofd.service.UserChangeEmployeeIdQueueService;
import lombok.extern.slf4j.Slf4j;

@RequireReadAccess
@Controller
@Slf4j
public class PersonController {

    @Autowired
    private PersonService personService;

    @Autowired
    private CreatePersonDTOValidator createPersonDTOValidator;

    @Autowired
    private AffiliationDTOValidator affiliationDTOValidator;

    @Autowired
    private OrgUnitService orgUnitService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private FunctionTypeService functionTypeService;

    @Autowired
    private AccountOrderService accountOrderService;

    @Autowired
    private SupportedUserTypeService supportedUserTypeService;

    @Autowired
    private SubstituteContextService substituteContextService;

    @Autowired
    private UserChangeEmployeeIdQueueService userChangeEmployeeIdQueueService;

    @Autowired
    private AuditLogger auditLogger;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SofdConfiguration configuration;

    @InitBinder("createPersonDTO")
    public void initClientBinder(WebDataBinder binder) {
        binder.setValidator(createPersonDTOValidator);
    }

    @InitBinder("affiliationDTO")
    public void initAffiliationBinder(WebDataBinder binder) {
        binder.setValidator(affiliationDTOValidator);
    }

    @RequirePersonCreaterOrControllerWriteAccess
    @GetMapping("/ui/person/new")
    public String newPerson(Model model) {
        CreatePersonDTO person = CreatePersonDTO.builder()
                .affiliation(AffiliationDTO.builder().affiliationType(AffiliationType.EMPLOYEE).build())
                .build();

        Set<String> constraintOUs = SecurityUtil.getOrgUnitUuidsConstraintFromPersonCreaterRole();
        boolean hasConstraints = false;
        if (!SecurityUtil.hasRole(RoleConstants.SYSTEM_ROLE_WRITE_ACCESS) && SecurityUtil.hasRole(RoleConstants.USER_ROLE_PERSON_CREATER) && !constraintOUs.isEmpty()) {
            hasConstraints = true;
        }

        model.addAttribute("hasConstraints", hasConstraints);
        model.addAttribute("constraintOus", constraintOUs);
        model.addAttribute("createPersonDTO", person);
        model.addAttribute("ous", orgUnitService.getAllTree());

        return "person/new";
    }

    @RequirePersonCreaterOrControllerWriteAccess
    @PostMapping("/ui/person/new")
    public String createPerson(Model model, @Valid @ModelAttribute("createPersonDTO") CreatePersonDTO createPersonDTO, BindingResult bindingResult) {
        Set<String> constraintOUs = SecurityUtil.getOrgUnitUuidsConstraintFromPersonCreaterRole();

        boolean hasConstraints = false;
        boolean choseOUNotInConstraints = false;
        if (!SecurityUtil.hasRole(RoleConstants.SYSTEM_ROLE_WRITE_ACCESS) && SecurityUtil.hasRole(RoleConstants.USER_ROLE_PERSON_CREATER) && !constraintOUs.isEmpty()) {
            hasConstraints = true;

            if (!constraintOUs.contains(createPersonDTO.getAffiliation().getOrgUnitUuid())) {
                choseOUNotInConstraints = true;
            }
        }

        if (bindingResult.hasErrors() || choseOUNotInConstraints) {
            model.addAttribute(bindingResult.getAllErrors());
            model.addAttribute("createPersonDTO", createPersonDTO);
            model.addAttribute("ous", orgUnitService.getAllTree());

            model.addAttribute("hasConstraints", hasConstraints);
            model.addAttribute("constraintOus", constraintOUs);
            model.addAttribute("choseOUNotInConstraints", choseOUNotInConstraints);

            return "person/new";
        }

        Person person = new Person();
        person.setCpr(createPersonDTO.getCpr());
        person.setUuid(UUID.randomUUID().toString());
        person.setMaster("SOFD");
        person.setFirstname(createPersonDTO.getFirstname());
        person.setSurname(createPersonDTO.getSurname());

        if (createPersonDTO.getRegisteredPostAddress() != null &&
                StringUtils.hasLength(createPersonDTO.getRegisteredPostAddress().getStreet()) &&
                StringUtils.hasLength(createPersonDTO.getRegisteredPostAddress().getPostalCode()) &&
                StringUtils.hasLength(createPersonDTO.getRegisteredPostAddress().getCity())) {
            Post post = new Post();
            post.setAddressProtected(createPersonDTO.getRegisteredPostAddress().isAddressProtected());
            post.setCity(createPersonDTO.getRegisteredPostAddress().getCity());
            post.setCountry(createPersonDTO.getRegisteredPostAddress().getCountry());
            post.setLocalname(createPersonDTO.getRegisteredPostAddress().getLocalname());
            post.setMaster("SOFD");
            post.setMasterId(UUID.randomUUID().toString());
            post.setPostalCode(createPersonDTO.getRegisteredPostAddress().getPostalCode());
            post.setPrime(true);
            post.setStreet(createPersonDTO.getRegisteredPostAddress().getStreet());

            person.setRegisteredPostAddress(post);
        }

        Affiliation aff = addAffiliationFromDTO(createPersonDTO.getAffiliation(), person);

        person = personService.save(person);

        var sb = new StringBuilder();
        sb.append("Person med tilhørsforhold oprettet via OS2sofd GUI. ");
        sb.append(person.getEntityLogInfo()).append(", ");
        sb.append("Startdato: ").append(aff.getStartDate()).append(", ");
        sb.append("Slutdato: ").append(aff.getStopDate()).append(", ");
        sb.append("Stillingsbetegnelse: ").append(aff.getPositionName()).append(", ");
        sb.append("Enhed: ").append(aff.getOrgUnit().getName());

        auditLogger.log(person.getUuid(), EntityType.PERSON, EventType.PERSON_CREATE, PersonService.getName(person), sb.toString());

        long affiliationId = 0;
        for (Affiliation affiliation : person.getAffiliations()) {
            if (affiliation.getMasterId().equals(aff.getMasterId())) {
                affiliationId = affiliation.getId();
                break;
            }
        }

        return "redirect:/ui/person/new/" + person.getUuid() + "/" + affiliationId;
    }

    @GetMapping("/ui/person/new/{uuid}/{affiliationId}")
    public String newReceipt(Model model, @PathVariable("uuid") String uuid, @PathVariable("affiliationId") long affiliationId) {
        Person person = personService.getByUuid(uuid);
        if (person == null) {
            return "redirect:/ui/person/";
        }

        String orgUnitName = "";
        String positionName = "";
        Affiliation affiliation = null;
        if (person.getAffiliations() != null && person.getAffiliations().size() > 0) {
            for (Affiliation aff : person.getAffiliations()) {
                if (aff.getId() == affiliationId) {
                    positionName = AffiliationService.getPositionName(aff);
                    orgUnitName = aff.getCalculatedOrgUnit().getName();
                    affiliation = aff;
                }
            }
        }

        List<FlaggedUserType> flaggedUserTypes = new ArrayList<>();
        for (SupportedUserType userType : supportedUserTypeService.findAll()) {
            if (!userType.isCanOrder()) {
                continue;
            }

            boolean flagged = accountOrderService.shouldOrderAccountOfType(userType.getKey(), affiliation, 0, false);

            FlaggedUserType flaggedUserType = new FlaggedUserType();
            flaggedUserType.setFlagged(flagged);
            flaggedUserType.setUserType(userType.getKey());
            flaggedUserType.setUserTypeName(userType.getName());

            flaggedUserTypes.add(flaggedUserType);
        }

        model.addAttribute("name", person.getFirstname() + " " + person.getSurname());
        model.addAttribute("orgUnitName", orgUnitName);
        model.addAttribute("positionName", positionName);
        model.addAttribute("userTypes", flaggedUserTypes);
        model.addAttribute("uuid", uuid);

        return "person/receipt";
    }

    @GetMapping("/ui/person")
    public String list(Model model) {
    	model.addAttribute("showPersonState", configuration.getIntegrations().getCpr().isShowPersonStateInUi());

        return "person/list";
    }

    @GetMapping("/ui/person/core/{uuid}/{type}")
    public String getCoreFragment(Model model, @PathVariable("uuid") String uuid, @PathVariable("type") String type) {
        Person person = personService.getByUuid(uuid);
        if (person == null) {
            log.warn("No person with uuid: " + uuid);
            return "redirect:/ui/person";
        }

        model.addAttribute("person", person);

        if (type.equals("edit")) {
            model.addAttribute("person", person);

            return "person/fragments/person_core_edit :: personCoreEdit";
        }

        return "person/fragments/person_core_view :: personCoreView";
    }

    @GetMapping("/ui/person/view/{uuid}")
    public String view(Model model, @PathVariable("uuid") String uuid, @RequestParam(required = false, value = "backRef") String backRef) {
        Person person = personService.getByUuid(uuid);
        if (person == null) {
            return "redirect:/ui/person/";
        }

        PersonDTO personDTO = toDTO(person);
        List<SimpleAffiliationDTO> affiliations = new ArrayList<SimpleAffiliationDTO>();
        List<String> functions = new ArrayList<>();
        Date today = new Date();
        for (Affiliation affiliation : AffiliationService.notStoppedAffiliations(person.getAffiliations())) {
            SimpleAffiliationDTO dto = new SimpleAffiliationDTO(affiliation);

            if (affiliation.getStartDate() != null) {
                dto.setActiveInFuture(dateWithoutTime(affiliation.getStartDate()).after(today));
            }
            else {
                dto.setActiveInFuture(false);
            }

            affiliations.add(dto);

            if (configuration.isShowOpusFunctionsOnPersonPage() && !dto.isActiveInFuture() && affiliation.getFunctions() != null) {
                functions.addAll(affiliation.getFunctions().stream().map(f -> f.getFunction()).collect(Collectors.toList()));
            }
        }

        Set<String> personUserTypes = PersonService.getUsers(person).stream().map(u -> u.getUserType()).collect(Collectors.toSet());
        List<SupportedUserTypeSlimDTO> supportedUserTypes = supportedUserTypeService.findAll().stream()
                .filter(u -> u.isCanOrder() && personUserTypes.contains(u.getKey()))
                .map(u -> new SupportedUserTypeSlimDTO(u))
                .collect(Collectors.toList());

        List<OrgUnit> managedOrgUnits = orgUnitService.getAllWhereManagerIs(person);

        model.addAttribute("supportedUserTypes", supportedUserTypes);
        model.addAttribute("affiliations", affiliations);
        model.addAttribute("phones", PersonService.getPhones(person).stream().map(p -> new PhoneDTO(p)).collect(Collectors.toList()));
        model.addAttribute("users", PersonService.getUsers(person).stream().map(u -> new PrimeUserDTO(u)).collect(Collectors.toList()));
        model.addAttribute("allUserTypes", supportedUserTypeService.findAll());
        model.addAttribute("functionTypes", functionTypeService.findAllAsDTO());
        model.addAttribute("backRef", backRef);
        model.addAttribute("showChoosePrimeAD", personDTO.getUsers().stream().filter(user -> SupportedUserTypeService.isActiveDirectory(user.getUserType())).count() > 1);
        model.addAttribute("today", LocalDate.now().toString());
        model.addAttribute("constraintOUs", SecurityUtil.getOrgUnitUuidsConstraintFromPersonCreaterRole());
        model.addAttribute("isManager", !managedOrgUnits.isEmpty());
        model.addAttribute("managedOrgUnits", managedOrgUnits);
        model.addAttribute("substituteContexts", substituteContextService.getAll());
        model.addAttribute("showFunctions", configuration.isShowOpusFunctionsOnPersonPage());
        model.addAttribute("functions", functions);
        model.addAttribute("primeManagerName", PersonService.getPrimeManager(person) != null ? PersonService.getPrimeManager(person).getName() : "");

        if (SecurityUtil.getUserRoles().contains(RoleConstants.MODULE_ROLE_ACCOUNT_CREATION)) {
            List<SupportedUserType> userTypes = new ArrayList<>();
            long affiliationCount = person.getAffiliations().size();
            long opusAffiliationCount = person.getAffiliations().stream().filter(a -> "OPUS".equals(a.getMaster())).count();

            // only show userTypes that can be ordered, and where any affiliation requirements are fulfilled,
            // meaning that when singleUserMode is disabled, we need at least one affiliation (to link with), and
            // in the case of OPUS accounts, we need the OPUS account to link with
            userTypes = supportedUserTypeService.findAll().stream()
                    .filter(u -> u.isCanOrder())
                    .filter(u ->
                            u.isSingleUserMode() ||
                                    (!SupportedUserTypeService.isOpus(u.getKey()) && affiliationCount > 0) ||
                                    (SupportedUserTypeService.isOpus(u.getKey()) && opusAffiliationCount > 0))
                    .collect(Collectors.toList());

            model.addAttribute("userTypes", userTypes);
            model.addAttribute("stoppableAffiliations", person.getAffiliations().stream().filter(a -> a.isDeleted() == false && !AffiliationService.notActiveAnymore(a)).collect(Collectors.toList()));

            boolean exchangeAvailable = userTypes.stream().anyMatch(u -> SupportedUserTypeService.isExchange(u.getKey()));
            boolean adAvailable = userTypes.stream().anyMatch(u -> SupportedUserTypeService.isActiveDirectory(u.getKey()));

            if (exchangeAvailable && adAvailable) {
                ExchangeAndADDTO dto = new ExchangeAndADDTO();

                dto.setKey("ACTIVE_DIRECTORY_AND_EXCHANGE");
                dto.setName("Active Directory + Exchange");

                model.addAttribute("activeDirectoryAndExchange", dto);
            }
        } else {
            model.addAttribute("userTypes", new ArrayList<SupportedUserType>());
            model.addAttribute("stoppableAffiliations", new ArrayList<Affiliation>());
        }

        if (!managedOrgUnits.isEmpty()) {
            var orgUnitAssignments = new HashMap<Long,SubstituteAssignmentDTO>();

            for (OrgUnit ou : managedOrgUnits) {
                for( var assignment : orgUnitService.getOrgUnitSubstitutes(ou) ) {
                    orgUnitAssignments.putIfAbsent(assignment.getId(),new SubstituteAssignmentDTO(assignment));
                }
            }
            personDTO.getSubstituteAssignments().addAll(orgUnitAssignments.values());
        }

        model.addAttribute("person", personDTO);

        return "person/view";
    }

    private Date dateWithoutTime(Date date) {
        Calendar calendar = Calendar.getInstance();

        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime();
    }

    @GetMapping("/ui/person/phoneTab/{uuid}")
    public String getPhoneTab(Model model, @PathVariable("uuid") String uuid) {
        Person person = personService.getByUuid(uuid);
        if (person == null) {
            log.warn("No Person with uuid: " + uuid);
            return "fragments/phoneTab :: phoneTab";
        }

        model.addAttribute("phones", PersonService.getPhones(person).stream().map(p -> new PhoneDTO(p)).collect(Collectors.toList()));

        return "fragments/phoneTab :: phoneTab";
    }


    @GetMapping("/ui/person/phonePrime/{uuid}")
    public String getPhonePrime(Model model, @PathVariable("uuid") String uuid) {
        Person person = personService.getByUuid(uuid);
        if (person == null) {
            log.warn("No Person with uuid: " + uuid);
            return "fragments/phoneTab :: phonePrimeModal";
        }

        model.addAttribute("phones", PersonService.getPhones(person).stream().map(p -> new PhoneDTO(p)).collect(Collectors.toList()));

        return "fragments/phoneTab :: phonePrimeModal";
    }


    @RequirePersonCreaterOrControllerWriteAccess
    @GetMapping(path = {"/ui/person/affiliation/{personUUID}"})
    public String orderNewUser(Model model, @PathVariable String personUUID, @RequestParam(required = false, value = "error") Boolean error) {
        AffiliationDTO affiliationDTO = new AffiliationDTO();
        affiliationDTO.setAffiliationType(AffiliationType.EMPLOYEE);

        Set<String> constraintOUs = SecurityUtil.getOrgUnitUuidsConstraintFromPersonCreaterRole();
        boolean hasConstraints = false;
        if (!SecurityUtil.hasRole(RoleConstants.SYSTEM_ROLE_WRITE_ACCESS) && SecurityUtil.hasRole(RoleConstants.USER_ROLE_PERSON_CREATER) && !constraintOUs.isEmpty()) {
            hasConstraints = true;
        }

        model.addAttribute("hasConstraints", hasConstraints);
        model.addAttribute("constraintOus", constraintOUs);
        model.addAttribute("affiliationDTO", affiliationDTO);
        model.addAttribute("personUUID", personUUID);
        model.addAttribute("ous", orgUnitService.getAllTree());
    	model.addAttribute("showError", error != null);

        return "person/new_affiliation";
    }

    @RequirePersonCreaterOrControllerWriteAccess
    @PostMapping(path = {"/ui/person/affiliation"})
    public String createNewAffiliation(Model model, @ModelAttribute("personUUID") String personUUID, @Valid @ModelAttribute("affiliationDTO") AffiliationDTO affiliationDTO, BindingResult bindingResult) {
        Set<String> constraintOUs = SecurityUtil.getOrgUnitUuidsConstraintFromPersonCreaterRole();

        boolean hasConstraints = false;
        boolean choseOUNotInConstraints = false;
        if (!SecurityUtil.hasRole(RoleConstants.SYSTEM_ROLE_WRITE_ACCESS) && SecurityUtil.hasRole(RoleConstants.USER_ROLE_PERSON_CREATER) && !constraintOUs.isEmpty()) {
            hasConstraints = true;

            if (!constraintOUs.contains(affiliationDTO.getOrgUnitUuid())) {
                choseOUNotInConstraints = true;
            }
        }

        if (bindingResult.hasErrors() || choseOUNotInConstraints) {
            model.addAttribute(bindingResult.getAllErrors());
            model.addAttribute("affiliationDTO", affiliationDTO);
            model.addAttribute("personUUID", personUUID);
            model.addAttribute("ous", orgUnitService.getAllTree());
            model.addAttribute("hasConstraints", hasConstraints);
            model.addAttribute("constraintOus", constraintOUs);
            model.addAttribute("choseOUNotInConstraints", choseOUNotInConstraints);

            return "person/new_affiliation";
        }

        Person person = personService.getByUuid(personUUID);
        if (person == null) {
            log.warn("Could not find person with uuid " + personUUID + " while assigning new affiliation");
            return "redirect:/ui/person/view/" + personUUID;
        }

        Affiliation aff = addAffiliationFromDTO(affiliationDTO, person);

        person = personService.save(person);

        long affiliationId = 0;
        Affiliation createdAffiliation = null;
        for (Affiliation affiliation : person.getAffiliations()) {
            if (affiliation.getMasterId().equals(aff.getMasterId())) {
                affiliationId = affiliation.getId();
                createdAffiliation = affiliation;
                break;
            }
        }

        if (affiliationId == 0) {
        	return "redirect:/ui/person/affiliation/" + personUUID + "?error=true";
        }

        // if affiliation has start date in the future - create a notification
        if (createdAffiliation.getStartDate() != null && createdAffiliation.getStartDate().after(new Date())) {
            createNewSofdAffiliationNotification(createdAffiliation);
        }

        var sb = new StringBuilder();
        sb.append("Tilhørsforhold oprettet via OS2sofd GUI. ");
        sb.append(person.getEntityLogInfo()).append(", ");
        sb.append("Startdato: ").append(aff.getStartDate()).append(", ");
        sb.append("Slutdato: ").append(aff.getStopDate()).append(", ");
        sb.append("Stillingsbetegnelse: ").append(aff.getPositionName()).append(", ");
        sb.append("Enhed: ").append(aff.getCalculatedOrgUnit().getName());
        auditLogger.log(person.getUuid(), EntityType.PERSON, EventType.AFFILIATION_CREATED, PersonService.getName(person), sb.toString());

        return "redirect:/ui/person/new/" + person.getUuid() + "/" + affiliationId;
    }

    @GetMapping("/ui/person/contacts/{uuid}/{type}")
    public String getContactInfoTab(Model model, @PathVariable("uuid") String uuid, @PathVariable("type") String type) {
        Person person = personService.getByUuid(uuid);
        if (person == null) {
            log.warn("No Person with uuid: " + uuid);

            return "person/fragments/viewKeywordsTab :: keywordsTab";
        }

        model.addAttribute("keyWords", person.getKeyWords());
        model.addAttribute("notes", person.getNotes());

        if (type.equals("edit")) {
            return "person/fragments/editKeywordsTab :: keywordsTab";
        }

        return "person/fragments/viewKeywordsTab :: keywordsTab";
    }

    @GetMapping("/ui/person/revision/list/{uuid}")
    public String personHistory(Model model, @PathVariable("uuid") String uuid) {
        List<RevisionId> revs = personService.getRevisionIds(uuid);
        model.addAttribute("revisions", revs);
        model.addAttribute("uuid", uuid);

        return "person/revision_list";
    }

    @GetMapping("/ui/person/revision/download/{uuid}/{revId}")
    public ResponseEntity<byte[]> downloadPersonHistory(@PathVariable("uuid") String uuid, @PathVariable("revId") Integer revId) {
        Revision<Integer, Person> revision = personService.findRevision(uuid, revId);
        String json = personService.getRevisionJson(revision);

        var timestamp = "";
        if (revision != null) {
            var sdf = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
            timestamp = sdf.format(Date.from(revision.getMetadata().getRequiredRevisionInstant()));
        }
        byte[] output = json.getBytes(StandardCharsets.UTF_8);

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("charset", "utf-8");
        responseHeaders.setContentType(MediaType.valueOf("application/json"));
        responseHeaders.setContentLength(output.length);
        responseHeaders.set("Content-disposition", "inline; filename=" + uuid + "-" + timestamp + ".json");

        return new ResponseEntity<byte[]>(output, responseHeaders, HttpStatus.OK);
    }

    @GetMapping("/ui/person/comments/{uuid}")
    public String getCommentsTab(Model model, @PathVariable("uuid") String uuid) {
        Person person = personService.getByUuid(uuid);
        if (person == null) {
            log.warn("No Person with uuid: " + uuid);

            return "person/fragments/viewCommentTab :: commentTab";
        }

        List<CommentDTO> comments = commentService.findByPersonUuid(uuid).stream()
                .map(c -> new CommentDTO(c))
                .sorted((o1, o2) -> (o1.getTimestamp().after(o2.getTimestamp())) ? -1 : 1)
                .collect(Collectors.toList());

        model.addAttribute("comments", comments);

        return "person/fragments/viewCommentTab :: commentTab";
    }

    private Affiliation addAffiliationFromDTO(AffiliationDTO affiliationDTO, Person person) {
        Affiliation affiliation = new Affiliation();
        affiliation.setUuid(UUID.randomUUID().toString());
        affiliation.setMaster("SOFD");
        affiliation.setMasterId(UUID.randomUUID().toString());
        affiliation.setOrgUnit(orgUnitService.getByUuid(affiliationDTO.getOrgUnitUuid()));
        affiliation.setPerson(person);
		affiliation.setPositionName((StringUtils.hasLength(affiliationDTO.getPositionName())) ? affiliationDTO.getPositionName().trim() : "Ukendt");
        affiliation.setAffiliationType(affiliationDTO.getAffiliationType());
        affiliation.setInternalReference(affiliationDTO.getInternalReference());

        if (affiliationDTO.getAffiliationType().equals(AffiliationType.EXTERNAL)) {
            affiliation.setInheritPrivileges(affiliationDTO.isInheritPrivilegesFromOU());
            affiliation.setVendor(affiliationDTO.getVendor());
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        if (StringUtils.hasLength(affiliationDTO.getStopDate())) {
            try {
                Date stopDate = formatter.parse(affiliationDTO.getStopDate());
                affiliation.setStopDate(stopDate);
            } catch (ParseException ex) {
                log.warn("Failed to parse: " + affiliationDTO.getStopDate());
            }
        }

        Date startDate = null;
        if (StringUtils.hasLength(affiliationDTO.getStartDate())) {
            try {
                startDate = formatter.parse(affiliationDTO.getStartDate());
                affiliation.setStartDate(startDate);
            } catch (ParseException ex) {
                log.warn("Failed to parse: " + affiliationDTO.getStartDate());
            }
        }

        if (affiliationDTO.isUseAsPrimaryWhenActive()) {
            // if no startDate or startDate is in the past set prime else set future prime field (useAsPrimaryWhenActive)
            Date now = new Date();
            if (startDate == null || startDate.before(now) || startDate.equals(now)) {
                if (person.getAffiliations() != null) {
                    for (Affiliation otherAffiliation : person.getAffiliations()) {
                        otherAffiliation.setSelectedPrime(false);
                    }
                }

                affiliation.setSelectedPrime(true);
            } else {
                affiliation.setUseAsPrimaryWhenActive(true);
            }
        }

        if (person.getAffiliations() == null) {
            person.setAffiliations(new ArrayList<>());
        }
        person.getAffiliations().add(affiliation);

        return affiliation;
    }

    private PersonDTO toDTO(Person person) {
        PersonDTO personDTO = PersonDTO.builder()
                .uuid(person.getUuid())
                .cpr(PersonService.maskCpr(person.getCpr()))
                .master(person.getMaster())
                .firstname(person.getFirstname())
                .surname(person.getSurname())
                .chosenName(person.getChosenName())
                .firstEmploymentDate(person.getFirstEmploymentDate())
                .anniversaryDate(person.getAnniversaryDate())
                .created(person.getCreated())
                .lastChanged(person.getLastChanged())
                .affiliations(person.getAffiliations())
                .users(toDTOUsers(person))
                .keyWords(person.getKeyWords())
                .notes(person.getNotes())
                .taxedPhone(person.isTaxedPhone())
                .forceStop(person.isForceStop())
                .stopReason(person.getStopReason())
                .disableAccountOrdersCreate(person.isDisableAccountOrdersCreate())
                .disableAccountOrdersDisable(person.isDisableAccountOrdersDisable())
                .disableAccountOrdersDelete(person.isDisableAccountOrdersDelete())
                .phones(PersonService.getPhones(person).stream().map(p -> new PhoneDTO(p)).collect(Collectors.toList()))
                .leave(person.getLeave() != null)
                .leaveReason((person.getLeave() != null) ? person.getLeave().getReason() : null)
                .leaveReasonText((person.getLeave() != null) ? person.getLeave().getReasonText() : null)
                .leaveStartDate((person.getLeave() != null) ? person.getLeave().getStartDate() : null)
                .leaveStopDate((person.getLeave() != null) ? person.getLeave().getStopDate() : null)
                .leaveExpireAccounts((person.getLeave() != null) ? person.getLeave().isExpireAccounts() : configuration.getModules().getPerson().isLeaveAutoCheckAD())
                .leaveDisableAccountOrders((person.getLeave() != null) ? person.getLeave().isDisableAccountOrders() : configuration.getModules().getPerson().isLeaveAutoCheckIdM())
                .substituteAssignments(person.getSubstitutes().stream().map(s -> new SubstituteAssignmentDTO(s)).collect(Collectors.toList()))
                .build();

        Post regAddr = person.getRegisteredPostAddress();
        if (regAddr != null) {
            personDTO.setRegisteredPostAddress(PostDTO.builder()
                    .street(regAddr.getStreet())
                    .localname(regAddr.getLocalname())
                    .postalCode(regAddr.getPostalCode())
                    .city(regAddr.getCity())
                    .country(regAddr.getCountry())
                    .addressProtected(regAddr.isAddressProtected())
                    .master(regAddr.getMaster())
                    .build());
        }

        Post resAddr = person.getResidencePostAddress();
        if (resAddr != null) {
            personDTO.setResidencePostAddress(PostDTO.builder()
                    .street(resAddr.getStreet())
                    .localname(resAddr.getLocalname())
                    .postalCode(resAddr.getPostalCode())
                    .city(resAddr.getCity())
                    .country(resAddr.getCountry())
                    .addressProtected(resAddr.isAddressProtected())
                    .master(resAddr.getMaster())
                    .build());
        }
        
        if (person.getAffiliations() != null) {
        	Affiliation affiliation = person.getAffiliations().stream().filter(a -> a.isPrime()).findFirst().orElse(null);
        	
        	if (affiliation != null && affiliation.getOrgUnit() != null) {
        		if (affiliation.getOrgUnit().getPostAddresses() != null) {
        			OrgUnitPostMapping postMapping = affiliation.getOrgUnit().getPostAddresses().stream().filter(p -> p.getPost().isPrime()).findFirst().orElse(null);
        			
        			if (postMapping != null) {
        				personDTO.setWorkAddress(postMapping.getPost().getAsOneLine());
        			}
        		}
        	}
        }

        // add pending UserAccount orders to the list of users
        List<AccountOrder> accountOrders = accountOrderService.getPendingAndBlockedCreateOrdersForPerson(person.getUuid());
        for (AccountOrder accountOrder : accountOrders) {
            UserDTO userDTO = new UserDTO();

            userDTO.setUserId("(afventer oprettelse)");
            userDTO.setUserType(accountOrder.getUserType());
            userDTO.setPending(true);

            if (personDTO.getUsers() == null) {
                personDTO.setUsers(new ArrayList<>());
            }

            personDTO.getUsers().add(userDTO);
        }

        // map authorizationCodes
        personDTO.setAuthorizationCodes(new ArrayList<>());
        if (person.getAuthorizationCodes() != null) {
        	for (AuthorizationCode authorizationCode : person.getAuthorizationCodes().stream().map(PersonAuthorizationCodeMapping::getAuthorizationCode).toList()) {
        		AuthorizationCodeDTO authorizationCodeDTO = new AuthorizationCodeDTO();
        		authorizationCodeDTO.setCode(authorizationCode.getCode());
        		authorizationCodeDTO.setId(authorizationCode.getId());
        		authorizationCodeDTO.setName(authorizationCode.getName());
        		authorizationCodeDTO.setPrime(authorizationCode.isPrime());

            	personDTO.getAuthorizationCodes().add(authorizationCodeDTO);
			}
        }

        // map localExtensions
        personDTO.setLocalExtensions(new ArrayList<>());
        if (person.getLocalExtensions() != null) {
            try {
                Map<String, String> someMap = new ObjectMapper().readValue(person.getLocalExtensions(), new TypeReference<Map<String, String>>() {
                });

                for (Entry<String, String> entry : someMap.entrySet()) {
                    LocalExtensionDTO le = new LocalExtensionDTO();

                    le.setType(LocalExtensionType.PERSON);
                    le.setField(entry.getKey());
                    le.setValue(entry.getValue());

                    personDTO.getLocalExtensions().add(le);
                }
            } catch (Exception ex) {
                LocalExtensionDTO le = new LocalExtensionDTO();

                le.setType(LocalExtensionType.PERSON);
                le.setField("ERROR");
                le.setValue(person.getLocalExtensions());

                personDTO.getLocalExtensions().add(le);
            }
        }

        // map user.localExtensions and check if kombit uuid is different from person uuid
        boolean differs = false;
        for (User user : PersonService.getUsers(person)) {
        	if (SupportedUserTypeService.isActiveDirectory(user.getUserType()) && user.getActiveDirectoryDetails() != null) {
        		if (!Objects.equals(user.getActiveDirectoryDetails().getKombitUuid(), person.getUuid())) {
        			differs = true;
        		}
        	}
        	
            if (user.getLocalExtensions() != null) {
                try {
                    Map<String, String> someMap = new ObjectMapper().readValue(user.getLocalExtensions(), new TypeReference<Map<String, String>>() {
                    });

                    for (Entry<String, String> entry : someMap.entrySet()) {
                        LocalExtensionDTO le = new LocalExtensionDTO();

                        le.setType(LocalExtensionType.USER);
                        le.setObject(user.getUserId());
                        le.setField(entry.getKey());
                        le.setValue(entry.getValue());

                        personDTO.getLocalExtensions().add(le);
                    }
                }
                catch (Exception ex) {
                    LocalExtensionDTO le = new LocalExtensionDTO();

                    le.setType(LocalExtensionType.USER);
                    le.setObject(user.getUserId());
                    le.setField("ERROR");
                    le.setValue(user.getLocalExtensions());

                    personDTO.getLocalExtensions().add(le);
                }
            }
        }
        
        personDTO.setPersonUuidNotKombitUuid(differs);

        // affiliation.localExtensions
        for (Affiliation aff : person.getAffiliations()) {
            if (aff.getLocalExtensions() != null) {
                try {
                    Map<String, String> someMap = new ObjectMapper().readValue(aff.getLocalExtensions(), new TypeReference<Map<String, String>>() {
                    });

                    for (Entry<String, String> entry : someMap.entrySet()) {
                        LocalExtensionDTO le = new LocalExtensionDTO();

                        le.setType(LocalExtensionType.AFFILIATION);                         
                        le.setObject(aff.getCalculatedOrgUnit().getName() + " (" + aff.getEmployeeId() + ")");
                        le.setField(entry.getKey());
                        le.setValue(entry.getValue());

                        personDTO.getLocalExtensions().add(le);
                    }
                } catch (Exception e) {
                    LocalExtensionDTO le = new LocalExtensionDTO();

                    le.setType(LocalExtensionType.AFFILIATION);
                    le.setObject(aff.getCalculatedOrgUnit().getName());
                    le.setField("ERROR");
                    le.setValue(aff.getLocalExtensions());

                    personDTO.getLocalExtensions().add(le);
                }
            }
        }

        return personDTO;
    }

    private List<UserDTO> toDTOUsers(Person person) {
        List<AccountOrder> accountOrdersPendingDeactivation = accountOrderService.findPendingDeactivation(person);
        List<UserDTO> userDTOs = new ArrayList<>();

        for (User user : PersonService.getUsers(person)) {
            ActiveDirectoryDetails activeDirectoryDetails = user.getActiveDirectoryDetails();

            UserDTO userDTO = new UserDTO();
            userDTO.setAccountExpireDate(activeDirectoryDetails != null ? activeDirectoryDetails.getAccountExpireDate() : null);
            userDTO.setDisabled(user.isDisabled());
            userDTO.setPasswordLocked(activeDirectoryDetails != null ? activeDirectoryDetails.isPasswordLocked() : false);
            userDTO.setPendingDeactivation(accountOrdersPendingDeactivation.stream()
                    .anyMatch(ao -> Objects.equals(ao.getUserType(), user.getUserType()) &&
                            Objects.equals(ao.getRequestedUserId(), user.getUserId())));
            userDTO.setPrime(user.isPrime());
            userDTO.setUserId(user.getUserId());
            userDTO.setUserType(user.getUserType());

            if (StringUtils.hasLength(user.getEmployeeId())) {
                userDTO.setEmployeeId(user.getEmployeeId());
                var mappedAffiliation = person.getAffiliations().stream().filter(a -> Objects.equals(a.getEmployeeId(), user.getEmployeeId())).findFirst().orElse(null);
                var employeeIdDisplay = mappedAffiliation == null ? "Ugyldigt medarbejdernummer" : AffiliationService.getPositionName(mappedAffiliation) + " i " + mappedAffiliation.getCalculatedOrgUnit().getName();
                userDTO.setEmployeeIdDisplay(employeeIdDisplay);
            }
            
            userDTO.setSubstituteAccount(user.isSubstituteAccount());
            userDTO.setUuid(user.getUuid());
            userDTO.setId(user.getId());
            userDTO.setPending(false);

            if (SupportedUserTypeService.isActiveDirectory(user.getUserType())) {
                UserChangeEmployeeIdQueue queue = userChangeEmployeeIdQueueService.findByUser(user);

                if (queue != null) {
                    UserChangeEmployeeIdQueueDTO userChangeEmployeeIdQueueDTO = new UserChangeEmployeeIdQueueDTO();
                    userChangeEmployeeIdQueueDTO.setDate(queue.getDateOfTransaction());
                    userChangeEmployeeIdQueueDTO.setEmployeeId(queue.getEmployeeId());

                    userDTO.setUserChangeEmployeeIdQueueDTO(userChangeEmployeeIdQueueDTO);
                }

                if (user.getActiveDirectoryDetails() != null) {
                    userDTO.setKombitUuid(user.getActiveDirectoryDetails().getKombitUuid());
                }
            }

            userDTOs.add(userDTO);
        }

        return userDTOs;
    }

    private void createNewSofdAffiliationNotification(Affiliation affiliation) {
        Notification notification = new Notification();
        notification.setActive(true);
        notification.setAffectedEntityUuid(affiliation.getPerson().getUuid());
        notification.setAffectedEntityType(EntityType.PERSON);
        notification.setAffectedEntityName(PersonService.getName(affiliation.getPerson()));
        notification.setNotificationType(NotificationType.NEW_AFFILIATION_SOFD);
        notification.setCreated(new Date());
        notification.setMessage("Nyt tilhørsforhold i " + affiliation.getCalculatedOrgUnit().getName() + " (" + affiliation.getCalculatedOrgUnit().getShortname() + ")");

        if (affiliation.getStartDate() != null) {
            notification.setEventDate(affiliation.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
        }

        notificationService.save(notification);
    }
}
