package dk.digitalidentity.sofd.controller.rest;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.admin.dto.TagDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.PostDTO;
import dk.digitalidentity.sofd.controller.rest.model.ContactDTO;
import dk.digitalidentity.sofd.controller.rest.model.OrgUnitCoreInfo;
import dk.digitalidentity.sofd.controller.rest.model.PhoneDTO;
import dk.digitalidentity.sofd.controller.rest.model.TryAccountOrderRulesResult;
import dk.digitalidentity.sofd.dao.model.AccountOrder;
import dk.digitalidentity.sofd.dao.model.Ean;
import dk.digitalidentity.sofd.dao.model.FunctionType;
import dk.digitalidentity.sofd.dao.model.ManagedTitle;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.OrgUnitAccountOrder;
import dk.digitalidentity.sofd.dao.model.OrgUnitTag;
import dk.digitalidentity.sofd.dao.model.Organisation;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.Phone;
import dk.digitalidentity.sofd.dao.model.Post;
import dk.digitalidentity.sofd.dao.model.Profession;
import dk.digitalidentity.sofd.dao.model.Tag;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderDeactivateAndDeleteRule;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderRule;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.EventType;
import dk.digitalidentity.sofd.dao.model.mapping.OrgUnitPhoneMapping;
import dk.digitalidentity.sofd.dao.model.mapping.OrgUnitPostMapping;
import dk.digitalidentity.sofd.dao.model.mapping.OrgUnitPrimaryKleMapping;
import dk.digitalidentity.sofd.dao.model.mapping.OrgUnitSecondaryKleMapping;
import dk.digitalidentity.sofd.dao.model.mapping.OrgUnitTertiaryKleMapping;
import dk.digitalidentity.sofd.log.AuditLogger;
import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.security.RequireControllerWriteAccess;
import dk.digitalidentity.sofd.security.RequireLosAdminAccess;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.security.RequireWriteContactInfoAccess;
import dk.digitalidentity.sofd.service.AccountOrderService;
import dk.digitalidentity.sofd.service.FunctionTypeService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.OrganisationService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.PrimeService;
import dk.digitalidentity.sofd.service.ProfessionService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import dk.digitalidentity.sofd.service.TagsService;
import dk.digitalidentity.sofd.service.model.OUTreeFormWithTags;
import dk.digitalidentity.sofd.telephony.controller.rest.dto.AutoCompleteResult;
import dk.digitalidentity.sofd.telephony.controller.rest.dto.ValueData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequireReadAccess
public class OrgUnitRestController {

	@Autowired
	private AuditLogger auditLogger;
	
	@Autowired
	private OrgUnitService orgUnitService;

	@Autowired
	private FunctionTypeService functionTypeService;

	@Autowired
	private AccountOrderService accountOrderService;

	@Autowired
	private SupportedUserTypeService supportedUserTypeService;

	@Autowired
	private OrganisationService organisationService;

	@Autowired
	private TagsService tagsService;

	@Autowired
	private PersonService personService;

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private PrimeService primeService;

	@Autowired
	private ProfessionService professionService;

	@RequireLosAdminAccess
	@DeleteMapping(value = "/rest/orgunit/{uuid}")
	@ResponseBody
	public HttpEntity<String> delete(@PathVariable("uuid") String uuid) throws Exception {
		var orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if (!orgUnitService.isDeletable(orgUnit)) {
			return new ResponseEntity<>("Orgunit is not deletable", HttpStatus.BAD_REQUEST);
		}

		orgUnit.setDeleted(true);
		orgUnitService.save(orgUnit);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireAdminAccess
	@PostMapping(value = "/rest/orgunit/{uuid}/copyRulesTo")
	@ResponseBody
	public HttpEntity<List<String>> copyRulesTo(@PathVariable("uuid") String uuid, @RequestBody List<String> uuids) {
		OrgUnit ou = orgUnitService.getByUuid(uuid);
		if (ou == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		OrgUnitAccountOrder accountOrder = accountOrderService.getAccountOrderSettings(ou, true);

		List<String> failedOrgUnits = new ArrayList<String>();

		List<OrgUnit> orgUnits = orgUnitService.getByUuid(uuids);
		for (OrgUnit orgUnit : orgUnits) {
			// copy-to-self should not trigger any kind of update ;)
			if (orgUnit.getUuid().equals(ou.getUuid())) {
				continue;
			}

			OrgUnitAccountOrder existingOrder = accountOrderService.getAccountOrderSettings(orgUnit, true);
			boolean byPositionRulesExist = existingOrder.getTypes().stream().anyMatch(t -> t.getRule().equals(AccountOrderRule.BY_POSITION_NAME));

			// do not overwrite rules on OrgUnits that has existing rules based on positions
			if (!byPositionRulesExist) {
				accountOrderService.setAccountOrderSettings(orgUnit, accountOrder, false);
			}
			else {
				failedOrgUnits.add(orgUnit.getName());
			}
		}

		return new ResponseEntity<>(failedOrgUnits, HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping(value = "/rest/orgunit/update/kle")
	@ResponseBody
	public HttpEntity<String> updateKle(@RequestHeader("uuid") String uuid, @RequestHeader("type") String type, @RequestBody List<String> codes) throws Exception {
		OrgUnit ou = orgUnitService.getByUuid(uuid);
		if (ou == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		if ("KlePrimary".equals(type)) {
			List<OrgUnitPrimaryKleMapping> existingKles = ou.getKlePrimary();

			// To remove
			for (Iterator<OrgUnitPrimaryKleMapping> iterator = existingKles.iterator(); iterator.hasNext();) {
				OrgUnitPrimaryKleMapping existingKle = iterator.next();

				if (!codes.contains(existingKle.getKleValue())) {
					iterator.remove();
				}
			}

			// To add
			Set<String> existingCodes = existingKles.stream().map(k -> k.getKleValue()).collect(Collectors.toSet());
			for (String code : codes) {
				if (!existingCodes.contains(code)) {
					OrgUnitPrimaryKleMapping mapping = new OrgUnitPrimaryKleMapping();
					mapping.setOrgUnit(ou);
					mapping.setKleValue(code);

					ou.getKlePrimary().add(mapping);
				}
			}
		}
		else if ("KleSecondary".equals(type)) {
			List<OrgUnitSecondaryKleMapping> existingKles = ou.getKleSecondary();

			// To remove
			for (Iterator<OrgUnitSecondaryKleMapping> iterator = existingKles.iterator(); iterator.hasNext();) {
				OrgUnitSecondaryKleMapping existingKle = iterator.next();

				if (!codes.contains(existingKle.getKleValue())) {
					iterator.remove();
				}
			}

			// To add
			Set<String> existingCodes = existingKles.stream().map(k -> k.getKleValue()).collect(Collectors.toSet());
			for (String code : codes) {
				if (!existingCodes.contains(code)) {
					OrgUnitSecondaryKleMapping mapping = new OrgUnitSecondaryKleMapping();
					mapping.setOrgUnit(ou);
					mapping.setKleValue(code);

					ou.getKleSecondary().add(mapping);
				}
			}
		}
		else if ("KleTertiary".equals(type)) {
			List<OrgUnitTertiaryKleMapping> existingKles = ou.getKleTertiary();

			// To remove
			for (Iterator<OrgUnitTertiaryKleMapping> iterator = existingKles.iterator(); iterator.hasNext();) {
				OrgUnitTertiaryKleMapping existingKle = iterator.next();

				if (!codes.contains(existingKle.getKleValue())) {
					iterator.remove();
				}
			}

			// To add
			Set<String> existingCodes = existingKles.stream().map(k -> k.getKleValue()).collect(Collectors.toSet());
			for (String code : codes) {
				if (!existingCodes.contains(code)) {
					OrgUnitTertiaryKleMapping mapping = new OrgUnitTertiaryKleMapping();
					mapping.setOrgUnit(ou);
					mapping.setKleValue(code);

					ou.getKleTertiary().add(mapping);
				}
			}
		}
		else {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		orgUnitService.save(ou);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping(value = "/rest/orgunit/update/kle/inherit")
	@ResponseBody
	public HttpEntity<String> updateKleInherit(@RequestHeader("uuid") String uuid, @RequestHeader("inherit") boolean inherit) throws Exception {
		OrgUnit ou = orgUnitService.getByUuid(uuid);
		if (ou == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		var kleInheritChanged = false;
		if( ou.isInheritKle() != inherit ) {
			kleInheritChanged = true;
		}
		ou.setInheritKle(inherit);
		orgUnitService.save(ou);

		// if inherit was changed we need to force an update on all children
		if( kleInheritChanged ) {
			orgUnitService.forceUpdateChildren(ou);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireAdminAccess
	@PostMapping(value = "/rest/orgunit/{uuid}/update/accountOrderRules/try")
	@ResponseBody
	public HttpEntity<TryAccountOrderRulesResult> tryUpdateAccountOrderRules(@PathVariable("uuid") String uuid, @RequestBody OrgUnitAccountOrder accountOrders) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		//Checks if the automatic account order rules and illegal coupled with non automatic compatible accountOrderDeactiveAndDelete
		List<AccountOrderRule> automaticAccountOrderRule = Arrays.asList(AccountOrderRule.EVERYONE, AccountOrderRule.EVERYONE_EXCEPT_HOURLY_PAID, AccountOrderRule.BY_POSITION_NAME);
		if (accountOrders.getTypes().stream().anyMatch(type -> automaticAccountOrderRule.contains(type.getRule()) && !type.getDeactivateAndDeleteRule().equals(AccountOrderDeactivateAndDeleteRule.KEEP_ALIVE))) {
			return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
		}

		TryAccountOrderRulesResult result = new TryAccountOrderRulesResult();
		result.setResult(new HashMap<>());

		List<AccountOrder> accountsToCreate = accountOrderService.getAccountsToCreate(orgUnit, accountOrders);
		for (AccountOrder order : accountsToCreate) {
			String userType = order.getUserType();

			// don't count orders for persons with disabled ordering
			Person person = personService.getByUuid(order.getPersonUuid());
			if (person != null && person.isDisableAccountOrdersCreate()) {
				continue;
			}

			if (result.getResult().containsKey(userType)) {
				result.getResult().put(userType, result.getResult().get(userType) + 1);
			}
			else {
				result.getResult().put(userType, 1L);
			}
		}

		Map<String, Long> prettyMap = new HashMap<>();

		// translate to prettier messages
		for (String key : result.getResult().keySet()) {
			prettyMap.put(supportedUserTypeService.getPrettyName(key), result.getResult().get(key));
		}
		result.setResult(prettyMap);

		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@RequireAdminAccess
	@PostMapping(value = "/rest/orgunit/{uuid}/update/accountOrderRules")
	@ResponseBody
	public HttpEntity<OrgUnitAccountOrder> updateAccountOrderRules(@PathVariable("uuid") String uuid, @RequestBody OrgUnitAccountOrder accountOrders) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		accountOrders = accountOrderService.setAccountOrderSettings(orgUnit, accountOrders, true);

		return new ResponseEntity<>(accountOrders, HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping("/rest/orgunit/{uuid}/update/coreInfo")
	public HttpEntity<?> updateCoreInformation(@PathVariable("uuid") String uuid, @RequestBody @Valid OrgUnitCoreInfo coreInfoDTO, BindingResult bindingResult) throws Exception {
		if (bindingResult.hasErrors()) {
			return new ResponseEntity<>(bindingResult.getAllErrors(), HttpStatus.BAD_REQUEST);
		}

		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		try {
			orgUnitService.updateCoreInformation(orgUnit, coreInfoDTO);
		}
		catch( Exception e ){
			log.warn("Failed to update core information", e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		String message = "Enheden " + orgUnit.getName() + " ændret.";
		auditLogger.log(orgUnit.getUuid(), EntityType.ORGUNIT, EventType.SAVE, orgUnit.getEntityName(), message);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireReadAccess
	@GetMapping(value = "/rest/orgunit/{uuid}/getPositionNames/autocomplete")
	@ResponseBody
	public ResponseEntity<?> searchPerson(@PathVariable("uuid") String uuid, @RequestParam("query") String term) {
		Set<String> titles = new HashSet<>();

		OrgUnit ou = orgUnitService.getByUuid(uuid);
		if (ou == null) {
			return new ResponseEntity<>(titles, HttpStatus.BAD_REQUEST);
		}

		// if any professions exist for chosen organization, we use professions as autocomplete
		titles = professionService.findAllCached().stream().filter(p -> p.getOrganisationId() == ou.getBelongsTo().getId()).map(Profession::getName).collect(Collectors.toSet());

		if( titles.isEmpty() ) {
			// otherwise use the old way (searching by existing positions in same orgunit)
			titles = orgUnitService.getPositionNames(ou, false);
			titles.addAll(ou.getManagedTitles().stream().map(m -> m.getName()).collect(Collectors.toSet()));
		}

		// filter - should contain search term
		titles = titles.stream().filter(t -> t.toLowerCase().contains(term.toLowerCase())).collect(Collectors.toSet());

		List<ValueData> suggestions = new ArrayList<>();
		for (String title : titles) {
			ValueData vd = new ValueData();
			vd.setValue(title);
			vd.setData(title);

			suggestions.add(vd);
		}

		AutoCompleteResult result = new AutoCompleteResult();
		result.setSuggestions(suggestions);

		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@RequireWriteContactInfoAccess
	@PostMapping(value = "/rest/orgunit/update/contactinfo")
	@ResponseBody
	public HttpEntity<ContactDTO> updateContactInfo(@RequestHeader("uuid") String uuid, @RequestBody ContactDTO contactInfo) throws Exception {
		OrgUnit ou = orgUnitService.getByUuid(uuid);
		if (ou == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		if (StringUtils.hasLength(contactInfo.getContactAddress()) && contactInfo.getContactAddress().length() > 250) {
			contactInfo.setContactAddress(contactInfo.getContactAddress().substring(0, 250));
		}
		else if (!StringUtils.hasLength(contactInfo.getContactAddress())) {
			contactInfo.setContactAddress(null);
		}

		ou.setContactAddress(contactInfo.getContactAddress());
		ou.setOpeningHours(contactInfo.getOpeningHours());
		ou.setOpeningHoursPhone(contactInfo.getOpeningHoursPhone());
		ou.setKeyWords(contactInfo.getKeywords());
		ou.setNotes(contactInfo.getNotes());
		ou.setEmailNotes(contactInfo.getEmailNotes());
		ou.setLocation(contactInfo.getLocation());
		ou.setUrlAddress(contactInfo.getUrlAddress());
		ou.setEmail(contactInfo.getEmail());
		orgUnitService.save(ou);

		return new ResponseEntity<>(contactInfo, HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping(value = "/rest/orgunit/editPhone")
	@ResponseBody
	public ResponseEntity<HttpStatus> editPhone(@RequestHeader("uuid") String uuid, @RequestBody PhoneDTO phoneDTO) throws Exception {
		if (!StringUtils.hasLength(phoneDTO.getPhoneNumber())) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
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

			OrgUnitPhoneMapping mapping = new OrgUnitPhoneMapping();
			mapping.setOrgUnit(orgUnit);
			mapping.setPhone(phone);

			orgUnit.getPhones().add(mapping);
		}
		else {
			List<Phone> phones = OrgUnitService.getPhones(orgUnit);
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

		orgUnitService.save(orgUnit);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping(path = { "/rest/orgunit/updatePrimaryPhones" })
	@ResponseBody
	public ResponseEntity<String> updatePrimaryPhones(@RequestHeader("uuid") String uuid, @RequestBody List<PhoneDTO> phoneDTOs) throws Exception {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		for (PhoneDTO phoneDTO : phoneDTOs) {
			Optional<Phone> first = OrgUnitService.getPhones(orgUnit).stream().filter(p -> p.getId() == phoneDTO.getId()).findFirst();
			if (first.isPresent()) {
				Phone phone = first.get();

				phone.setPrime(phoneDTO.isPrime());
				phone.setTypePrime(phoneDTO.isTypePrime());
			}
			else {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
		}

		orgUnitService.save(orgUnit);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping("/rest/orgunit/deletePhone")
	@ResponseBody
	public ResponseEntity<HttpStatus> deletePhone(@RequestHeader("uuid") String uuid, @RequestHeader("id") long id) throws Exception {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		boolean removedAny = orgUnit.getPhones().removeIf(phoneMapping -> phoneMapping.getPhone().getId() == id);

		if (removedAny) {
			orgUnitService.save(orgUnit);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireLosAdminAccess
	@PostMapping("/rest/orgunit/deletePost")
	@ResponseBody
	public ResponseEntity<HttpStatus> deletePost(@RequestHeader("uuid") String uuid, @RequestHeader("id") long id) throws Exception {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		boolean removedAny = orgUnit.getPostAddresses().removeIf(postMapping -> postMapping.getPost().getId() == id);
		List<Post> allPosts = OrgUnitService.getPosts(orgUnit);

		if (removedAny) {
			chooseDefaultReturnAddress(allPosts);

			orgUnitService.save(orgUnit);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireLosAdminAccess
	@PostMapping(value = "/rest/orgunit/editOrCreatePost")
	@ResponseBody
	public ResponseEntity<?> editOrCreatePost(@RequestHeader("uuid") String uuid, @RequestBody @Valid PostDTO postDTO, BindingResult bindingResult) throws Exception {
		if (bindingResult.hasErrors()) {
			return new ResponseEntity<>(bindingResult.getAllErrors(), HttpStatus.BAD_REQUEST);
		}

		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		List<Post> allPosts = OrgUnitService.getPosts(orgUnit);

		boolean changes = false;
		if (postDTO.getId() == 0) {
			Post post = new Post();
			post.setMaster("SOFD");
			post.setMasterId(UUID.randomUUID().toString());
			post.setCity(postDTO.getCity());
			post.setCountry(postDTO.getCountry());
			post.setLocalname(postDTO.getLocalname());
			post.setPostalCode(postDTO.getPostalCode());
			post.setStreet(postDTO.getStreet());

			if (postDTO.isReturnAddress()) {
				post.setReturnAddress(true);
				allPosts.forEach(p -> p.setReturnAddress(false));
			}

			if(postDTO.isPrime()) {
				post.setPrime(true);
				allPosts.forEach(p -> p.setPrime(false));
			}

			OrgUnitPostMapping mapping = new OrgUnitPostMapping();
			mapping.setOrgUnit(orgUnit);
			mapping.setPost(post);

			orgUnit.getPostAddresses().add(mapping);
			allPosts.add(post);

			changes = true;
		}
		else {
			Optional<Post> existingPost = allPosts.stream().filter(p -> p.getId() == postDTO.getId()).findFirst();
			if (existingPost.isPresent()) {
				Post modifiedPost = existingPost.get();

				if (modifiedPost.getMaster().equals("SOFD")) {

					if (!Objects.equals(modifiedPost.getCity(), postDTO.getCity())) {
						modifiedPost.setCity(postDTO.getCity());
						changes = true;
					}

					if (!Objects.equals(modifiedPost.getCountry(), postDTO.getCountry())) {
						modifiedPost.setCountry(postDTO.getCountry());
						changes = true;
					}

					if (!Objects.equals(modifiedPost.getLocalname(), postDTO.getLocalname())) {
						modifiedPost.setLocalname(postDTO.getLocalname());
						changes = true;
					}

					if (!Objects.equals(modifiedPost.getPostalCode(), postDTO.getPostalCode())) {
						modifiedPost.setPostalCode(postDTO.getPostalCode());
						changes = true;
					}

					if (!Objects.equals(modifiedPost.getStreet(), postDTO.getStreet())) {
						modifiedPost.setStreet(postDTO.getStreet());
						changes = true;
					}
				}

				if (!Objects.equals(modifiedPost.isReturnAddress(), postDTO.isReturnAddress())) {
					modifiedPost.setReturnAddress(postDTO.isReturnAddress());
					if (postDTO.isReturnAddress()) {
						allPosts.stream().filter(p -> p.getId() != modifiedPost.getId()).forEach(p -> p.setReturnAddress(false));
					}
					changes = true;
				}

				if (!Objects.equals(modifiedPost.isPrime(), postDTO.isPrime())) {
					modifiedPost.setPrime(postDTO.isPrime());
					if (postDTO.isPrime()) {
						allPosts.stream().filter(p -> p.getId() != modifiedPost.getId()).forEach(p -> p.setPrime(false));
					}
					changes = true;
				}
			}
			else {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
		}

		chooseDefaultReturnAddress(allPosts);

		if (changes) {
			orgUnitService.save(orgUnit);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	private void chooseDefaultReturnAddress(List<Post> allPosts) {
		if (!configuration.getModules().getOrgUnit().isChooseDefaultReturnAddressEnabled()) {
			return;
		}

		if (allPosts.stream().noneMatch(p -> p.isReturnAddress())) {
			Post primeAddress = allPosts.stream().filter(p -> p.isPrime()).findAny().orElse(null);
			if (primeAddress != null) {
				primeAddress.setReturnAddress(true);
			}
			else {
				Post anyAddress = allPosts.stream().findAny().orElse(null);
				if (anyAddress != null) {
					anyAddress.setReturnAddress(true);
				}
			}
		}
	}

	@RequireControllerWriteAccess
	@PostMapping(value = "/rest/orgunit/{uuid}/saveTag")
	@ResponseBody
	public ResponseEntity<?> saveTag(@PathVariable("uuid") String uuid, @RequestBody TagDTO tagDTO) throws Exception {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			return ResponseEntity.badRequest().build();
		}

		Tag tag = tagsService.findById(tagDTO.getId());
		if (tag == null) {
			return ResponseEntity.badRequest().build();
		}

		Optional<OrgUnitTag> exisingTag = orgUnit.getTags().stream().filter(t -> t.getTag().getId() == tag.getId()).findFirst();
		OrgUnitTag orgUnitTag;
		if (exisingTag.isPresent()) {
			orgUnitTag = exisingTag.get();
		}
		else {
			orgUnitTag = new OrgUnitTag();
			orgUnitTag.setOrgUnit(orgUnit);
			orgUnitTag.setTag(tag);
			orgUnit.getTags().add(orgUnitTag);
		}
		if (tag.isCustomValueEnabled()) {
			// verify regex tag syntax
			if (tag.getCustomValueRegex() != null && StringUtils.hasLength(tag.getCustomValueRegex())) {
				if (tagDTO.getCustomValue() == null || !Pattern.matches(tag.getCustomValueRegex(), tagDTO.getCustomValue())) {
					return new ResponseEntity<>("Ugyldig værdi", HttpStatus.BAD_REQUEST);
				}
			}
			// check for uniqueness if enabled for this tag
			if (tag.isCustomValueUnique()) {
				Optional<OrgUnitTag> exising = tag.getOrgUnitTags().stream().filter(t -> t.getId() != orgUnitTag.getId() && Objects.equals(t.getCustomValue(), tagDTO.getCustomValue())).findFirst();
				if (exising.isPresent()) {
					String errorMessage = String.format("Enheden \"%s\" er allerede opmærket med %s - %s: %s", exising.get().getOrgUnit().getName(), exising.get().getTag().getValue(), exising.get().getTag().getCustomValueName(), tagDTO.getCustomValue());
					return new ResponseEntity<>(errorMessage, HttpStatus.BAD_REQUEST);
				}
			}

			orgUnitTag.setCustomValue(tagDTO.getCustomValue());
		}
		else {
			orgUnitTag.setCustomValue(null);
		}
		orgUnitService.save(orgUnit);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping("/rest/orgunit/deleteTag")
	@ResponseBody
	public ResponseEntity<HttpStatus> deleteTag(@RequestHeader("uuid") String uuid, @RequestHeader("id") long id) throws Exception {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		boolean removedAny = orgUnit.getTags().removeIf(tag -> tag.getTag().getId() == id);

		if (removedAny) {
			orgUnitService.save(orgUnit);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireLosAdminAccess
	@PostMapping(value = "/rest/orgunit/new")
	@ResponseBody
	public ResponseEntity<?> createOrgUnit(@RequestBody @Valid OrgUnitCoreInfo orgUnitDTO, BindingResult bindingResult) throws Exception {
		if (bindingResult.hasErrors()) {
			return new ResponseEntity<>(bindingResult.getAllErrors(), HttpStatus.BAD_REQUEST);
		}
		Long belongsTo = orgUnitDTO.getBelongsTo();

		if (belongsTo == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		Organisation organisation = organisationService.getById(belongsTo);
		if (organisation == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		OrgUnit parent = orgUnitService.getByUuid(orgUnitDTO.getParent());
		if (parent == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		if (StringUtils.hasText(orgUnitDTO.getMasterId())) {
			OrgUnit existingOrgUnit = orgUnitService.getByMasterId(orgUnitDTO.getMasterId());
			if (existingOrgUnit != null) {
				return new ResponseEntity<>("Der findes allerede en enhed med det Kilde Id", HttpStatus.BAD_REQUEST);
			}
		}

		OrgUnit orgUnit = new OrgUnit();
		orgUnit.setSourceName(orgUnitDTO.getSourceName());
		orgUnit.setShortname(orgUnitDTO.getShortname());
		orgUnit.setCvr(orgUnitDTO.getCvr());
		orgUnit.setSenr(orgUnitDTO.getSenr());
		orgUnit.setPnr(orgUnitDTO.getPnr());
		orgUnit.setMaster("SOFD");
		orgUnit.setMasterId(StringUtils.hasText(orgUnitDTO.getMasterId()) ? orgUnitDTO.getMasterId().trim() : "SOFD-" + UUID.randomUUID());
		orgUnit.setParent(parent);
		orgUnit.setType(orgUnitService.getDepartmentType());
		orgUnit.setBelongsTo(organisation);
		orgUnit.setPostAddresses(new ArrayList<>());
		orgUnit.setDoNotTransferToFkOrg(orgUnitDTO.isDoNotTransferToFKOrg());

		if (orgUnitDTO.getPnr() != null) {
			Post post = new Post();
			post.setPrime(true);
			post.setStreet(orgUnitDTO.getStreet());
			post.setPostalCode(orgUnitDTO.getPostalCode());
			post.setCity(orgUnitDTO.getCity());
			post.setCountry("Danmark");
			post.setAddressProtected(false);
			post.setMaster("CVR");
			post.setMasterId(orgUnitDTO.getPnr().toString());

			OrgUnitPostMapping mapping = new OrgUnitPostMapping();
			mapping.setOrgUnit(orgUnit);
			mapping.setPost(post);

			orgUnit.getPostAddresses().add(mapping);
		}

		OrgUnit newOrgUnit = orgUnitService.save(orgUnit);
		
		String message = "Enheden " + newOrgUnit.getName() + " oprettet.";
		auditLogger.log(newOrgUnit.getUuid(), EntityType.ORGUNIT, EventType.SAVE, newOrgUnit.getEntityName(), message);

		return new ResponseEntity<>(newOrgUnit.getUuid(), HttpStatus.OK);
	}

	@RequireReadAccess
	@GetMapping(value = "/rest/orgunit/get-by-org/{id}")
	@ResponseBody
	public ResponseEntity<List<OUTreeFormWithTags>> getOUsByOrg(@PathVariable("id") Long id) {
		Organisation organisation = organisationService.getById(id);
		if (organisation == null) {
			return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
		}

		List<OUTreeFormWithTags> orgUnits = orgUnitService.getAllTreeWithTags(organisation);

		return new ResponseEntity<>(orgUnits, HttpStatus.OK);
	}

	@RequireLosAdminAccess
	@GetMapping(value = "/rest/orgunit/check")
	@ResponseBody
	public ResponseEntity<Boolean> checkName(@RequestParam String sourceName) {
		if (sourceName == null || sourceName.equals("")) {
			return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
		}

		boolean found = !orgUnitService.getBySourceName(sourceName).isEmpty();

		return new ResponseEntity<>(found, HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping(value = "/rest/orgunit/{uuid}/managedtitles/create")
	@ResponseBody
	public HttpEntity<String> createManagedTitle(@PathVariable("uuid") String uuid, @RequestBody String titleName) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			log.warn("No OrgUnit with uuid " + uuid);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		List<String> extraTitleNames = orgUnit.getManagedTitles().stream().map(m -> m.getName()).collect(Collectors.toList());
		if (!extraTitleNames.contains(titleName)) {
			ManagedTitle managedTitle = new ManagedTitle();
			managedTitle.setMaster("SOFD");
			managedTitle.setName(titleName);
			managedTitle.setOrgUnit(orgUnit);

			orgUnit.getManagedTitles().add(managedTitle);
			try {
				orgUnitService.save(orgUnit);
			}
			catch (Exception ex) {
				log.error("Failed to save orgUnit " + orgUnit.getUuid(), ex);
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
		}
		else {
			return new ResponseEntity<>("Der findes allerede en stilling på enheden med samme navn.", HttpStatus.BAD_REQUEST);
		}
		
		String message = "Stillingen " + titleName + " oprettet i " + orgUnit.getName() + ".";
		auditLogger.log(orgUnit.getUuid(), EntityType.MANAGED_TITLE, EventType.SAVE, orgUnit.getEntityName(), message);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping(value = "/rest/orgunit/{uuid}/managedtitles/{id}/delete")
	public HttpEntity<String> deleteManagedTitle(@PathVariable("uuid") String uuid, @PathVariable("id") long id) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			log.warn("No OrgUnit with uuid " + uuid);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		ManagedTitle titleToDelete = orgUnit.getManagedTitles().stream().filter(m -> m.getId() == id).findAny().orElse(null);
		if (titleToDelete == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if (!titleToDelete.getMaster().equals("SOFD")) {
			log.warn("ManagedTitle with id " + id + " is not owned by SOFD!");
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		orgUnit.getManagedTitles().remove(titleToDelete);
		try {
			orgUnitService.save(orgUnit);
		}
		catch (Exception ex) {
			log.error("Failed to save orgUnit " + orgUnit.getUuid(), ex);

			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		
		String message = "Stillingen " + titleToDelete.getName() + " slettet i " + orgUnit.getName() + ".";
		auditLogger.log(orgUnit.getUuid(), EntityType.MANAGED_TITLE, EventType.DELETE, orgUnit.getEntityName(), message);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping(value = "/rest/orgunit/{uuid}/ean/create")
	@ResponseBody
	public HttpEntity<String> createEAN(@PathVariable("uuid") String uuid, @RequestBody Long number) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			log.warn("No OrgUnit with uuid " + uuid);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		if (!Objects.equals(orgUnit.getMaster(), "SOFD")) {
			return new ResponseEntity<>("Kan kun ændre EAN på enheder oprettet i OS2sofd", HttpStatus.BAD_REQUEST);			
		}

		if (number == null) {
			return new ResponseEntity<>("EAN kan ikke være tom", HttpStatus.BAD_REQUEST);
		}

		List<Long> assignedEans = orgUnit.getEanList().stream().map(Ean::getNumber).toList();
		if (!assignedEans.contains(number)) {
			Ean ean = new Ean();
			ean.setMaster("SOFD");
			ean.setNumber(number);
			ean.setOrgUnit(orgUnit);
			ean.setPrime(false);

			orgUnit.getEanList().add(ean);
			primeService.setPrimeEan(orgUnit);
			try {
				orgUnitService.save(orgUnit);
			}
			catch (Exception ex) {
				log.error("Failed to save orgUnit " + orgUnit.getUuid(), ex);
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
		}
		else {
			return new ResponseEntity<>("Dette EAN nummer er allerede på enheden.", HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping(value = "/rest/orgunit/{uuid}/ean/setPrime")
	@ResponseBody
	public HttpEntity<String> setEanPrime(@PathVariable("uuid") String uuid, @RequestBody int id) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			log.warn("No OrgUnit with uuid " + uuid);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		if (!Objects.equals(orgUnit.getMaster(), "SOFD")) {
			return new ResponseEntity<>("Kan kun ændre EAN på enheder oprettet i OS2sofd", HttpStatus.BAD_REQUEST);			
		}

		Ean ean = orgUnit.getEanList().stream().filter(x -> x.getId() == id).findAny().orElse(null);
		if (ean == null) {
			return new ResponseEntity<>("Kunne ikke finde det angive EAN nummer", HttpStatus.BAD_REQUEST);
		}

		orgUnit.getEanList().forEach(p -> p.setPrime(false));
		ean.setPrime(true);

		try {
			orgUnitService.save(orgUnit);
		}
		catch (Exception ex) {
			log.error("Failed to save orgUnit " + orgUnit.getUuid(), ex);

			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@RequireControllerWriteAccess
	@PostMapping(value = "/rest/orgunit/{uuid}/ean/{id}/delete")
	public HttpEntity<String> deleteEAN(@PathVariable("uuid") String uuid, @PathVariable("id") long id) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			log.warn("No OrgUnit with uuid " + uuid);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if (!Objects.equals(orgUnit.getMaster(), "SOFD")) {
			return new ResponseEntity<>("Kan kun ændre EAN på enheder oprettet i OS2sofd", HttpStatus.BAD_REQUEST);			
		}

		Ean eanToDelete = orgUnit.getEanList().stream().filter(m -> m.getId() == id).findAny().orElse(null);
		if (eanToDelete == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if (!eanToDelete.getMaster().equals("SOFD")) {
			log.warn("EAN with id " + id + " is not owned by SOFD!");
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		orgUnit.getEanList().remove(eanToDelete);
		primeService.setPrimeEan(orgUnit);

		try {
			orgUnitService.save(orgUnit);
		}
		catch (Exception ex) {
			log.error("Failed to save orgUnit " + orgUnit.getUuid(), ex);

			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
