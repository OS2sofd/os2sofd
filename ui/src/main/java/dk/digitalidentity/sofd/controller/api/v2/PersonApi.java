package dk.digitalidentity.sofd.controller.api.v2;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.api.v2.model.PersonApiRecord;
import dk.digitalidentity.sofd.controller.api.v2.model.PersonResult;
import dk.digitalidentity.sofd.controller.api.v2.model.validator.PersonApiRecordValidator;
import dk.digitalidentity.sofd.dao.model.ActiveDirectoryDetails;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.MasteredEntity;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.Phone;
import dk.digitalidentity.sofd.dao.model.Post;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderStatus;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderType;
import dk.digitalidentity.sofd.dao.model.mapping.AffiliationFunctionMapping;
import dk.digitalidentity.sofd.dao.model.mapping.MappedEntity;
import dk.digitalidentity.sofd.dao.model.mapping.PersonPhoneMapping;
import dk.digitalidentity.sofd.dao.model.mapping.PersonUserMapping;
import dk.digitalidentity.sofd.security.RequireApiWriteAccess;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.AccountOrderService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import dk.digitalidentity.sofd.service.UserService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequireReadAccess
public class PersonApi {
	private static LocalDate year9999 = LocalDate.of(9999, 12, 31);

	@Autowired
	private PersonService personService;

	@Autowired
	private SofdConfiguration sofdConfiguration;

	@Autowired
	private AccountOrderService accountOrderService;

	@Autowired
	private PersonApiRecordValidator personValidator;

	@InitBinder
	protected void initBinder(WebDataBinder binder) {
		binder.addValidators(personValidator);
	}

	@GetMapping("/api/v2/persons")
	public PersonResult getPersons(@RequestParam(name = "page", defaultValue = "0") int page, @RequestParam(name = "offset", required = false, defaultValue = "") String offset, @RequestParam(name = "size", defaultValue = "100") int size) {
		List<Person> persons = null;
		
		if (!StringUtils.hasText(offset) && page > 0) {
			log.warn("/api/v2/persons called with page - this is bad performance and the caller should switch to offset");
			persons = personService.getAll(PageRequest.of(page, size, Sort.by("uuid"))).getContent();
		}
		else {
			persons = personService.getByOffsetAndLimit(offset, size);
		}

		PersonResult result = new PersonResult();
		result.setPersons(new HashSet<PersonApiRecord>());
		result.setPage(page);
		if (persons.size() > 0) {
			result.setNextOffset(persons.get(persons.size() - 1).getUuid());
		}

		for (Person person : persons) {
			result.getPersons().add(new PersonApiRecord(person));
		}

		return result;
	}

	@GetMapping("/api/v2/persons/byCpr/{cpr}")
	public ResponseEntity<?> getPersonByCpr(@PathVariable("cpr") String cpr) {
		Person person = personService.findByCpr(cpr);
		if (person == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		return new ResponseEntity<>(new PersonApiRecord(person), HttpStatus.OK);		
	}

	@GetMapping("/api/v2/persons/byADUserId/{userId}")
	public ResponseEntity<?> getPersonByADUserId(@PathVariable("userId") String userId) {
		List<Person> people = personService.findByUserTypeAndUserId(SupportedUserTypeService.getActiveDirectoryUserType(), userId);
		if (people.isEmpty()) {	
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		return new ResponseEntity<>(new PersonApiRecord(people.get(0)), HttpStatus.OK);
	}

	@GetMapping("/api/v2/persons/byKombitUuid/{kombitUuid}")
	public ResponseEntity<?> getPersonByKombitUuid(@PathVariable("kombitUuid") String kombitUuid) {
		var person = personService.findByKombitUuid(kombitUuid);
		if (person == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		return new ResponseEntity<>(new PersonApiRecord(person), HttpStatus.OK);
	}

	@GetMapping("/api/v2/persons/{uuid}")
	public ResponseEntity<?> getPerson(@PathVariable("uuid") String uuid) {
		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
		
		return new ResponseEntity<>(new PersonApiRecord(person), HttpStatus.OK);
	}

	// various integrations may need to know about these settings
	public record PersonApiSettings(boolean activeDirectoryEmployeeIdAssociationEnabled) { }

	@GetMapping("/api/v2/persons/settings")
	public ResponseEntity<?> getSettings() {
		return new ResponseEntity<>(
			new PersonApiSettings(
				sofdConfiguration.getIntegrations().getOpus().isEnableActiveDirectoryEmployeeIdAssociation()
			),
			HttpStatus.OK
		);
	}
	
	@RequireApiWriteAccess
	@PostMapping("/api/v2/persons")
	public ResponseEntity<?> createPerson(@Valid @RequestBody PersonApiRecord record, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			log.warn("Binding errors: " + bindingResult.getAllErrors());
			return new ResponseEntity<>(bindingResult.getAllErrors(), HttpStatus.BAD_REQUEST);
		}

		if (personService.getByUuid(record.getUuid()) != null) {
			return new ResponseEntity<>("Already exists", HttpStatus.CONFLICT);
		}

		String seedPrefix = null;
		if (!sofdConfiguration.getIntegrations().getOs2sync().isUseObjectGuidAsKombitUuid()) {
			seedPrefix = sofdConfiguration.getCustomer().getCvr() + record.getCpr();
		}


		Person person = record.toPerson(null, seedPrefix);
		checkForADUserWithSameUserId(person);

		person = personService.save(record.toPerson(null, seedPrefix));

		return new ResponseEntity<>(new PersonApiRecord(person), HttpStatus.CREATED);
	}

	@RequireApiWriteAccess
	@PatchMapping("/api/v2/persons/{uuid}")
	public ResponseEntity<?> patchPerson(@PathVariable("uuid") String uuid, @RequestBody PersonApiRecord record, BindingResult bindingResult) throws Exception {

		if (bindingResult.hasErrors()) {
			log.warn("Binding errors: " + bindingResult.getAllErrors());
			return new ResponseEntity<>(bindingResult.getAllErrors(), HttpStatus.BAD_REQUEST);
		}

		try {
			Person person = personService.getByUuid(uuid);
			if (person == null) {
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);
			}
			
			boolean changes = patch(person, record);

			if (changes) {
				person = personService.save(person);
			}
			
			if (!changes) {
				return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
			}

			return new ResponseEntity<>(new PersonApiRecord(person), HttpStatus.OK);
		}
		catch (Exception ex) {
			log.error("Failed to patch " + uuid + " with payload from client " + ((SecurityUtil.getClient() != null) ? SecurityUtil.getClient().getId() : "-1") + " - payload = " + record.toString());
			// let Spring map the exception to a HTTP 500
			throw ex;
		}
	}

	private boolean patch(Person person, PersonApiRecord personRecord) throws Exception {
		String seedPrefix = null;
		if (!sofdConfiguration.getIntegrations().getOs2sync().isUseObjectGuidAsKombitUuid()) {
			seedPrefix = sofdConfiguration.getCustomer().getCvr() + personRecord.getCpr();
		}

		Person record = personRecord.toPerson(person, seedPrefix);
		boolean changes = false;
		
		// in patch() fields are only updated if the supplied record is non-null, meaning PATCH cannot
		// null a field - a PUT operation must be implemented for null'ing to be possible.

		// comparison should be in date, not full object
		if (record.getAnniversaryDate() != null && !Objects.equals(toLocalDate(record.getAnniversaryDate()), toLocalDate(person.getAnniversaryDate()))) {
			person.setAnniversaryDate(record.getAnniversaryDate());
			changes = true;
		}

		if (!sofdConfiguration.getModules().getPerson().isChosenNameEditable()) {
			if (record.getChosenName() != null && !Objects.equals(record.getChosenName(), person.getChosenName())) {
				person.setChosenName(record.getChosenName());
				changes = true;
			}
		}
		
		if (record.getFirstEmploymentDate() != null && !Objects.equals(toLocalDate(record.getFirstEmploymentDate()), toLocalDate(person.getFirstEmploymentDate()))) {
			person.setFirstEmploymentDate(record.getFirstEmploymentDate());
			changes = true;
		}
		
		if (record.getFirstname() != null && !Objects.equals(record.getFirstname(), person.getFirstname())) {
			person.setFirstname(record.getFirstname());
			changes = true;
		}
		
		if (record.getLocalExtensions() != null && !Objects.equals(record.getLocalExtensions(), person.getLocalExtensions())) {
			person.setLocalExtensions(record.getLocalExtensions());
			changes = true;
		}
		
		if (record.getMaster() != null && !Objects.equals(record.getMaster(), person.getMaster())) {
			person.setMaster(record.getMaster());
			changes = true;
		}
	
		if (record.getSurname() != null && !Objects.equals(record.getSurname(), person.getSurname())) {
			person.setSurname(record.getSurname());
			changes = true;
		}
		
		// due to the way patching works, it is not possible "null" a collection using the PATCH operation,
		// an empty collection must be supplied to "empty" it.
		
		if (record.getUsers() != null) {
			// check for any new AD users here and supply them with employee_id from matching account order - if any exists.
			// note that this can fail due to timing between Event Dispatcher and Account Agent - in that case logic is handled in the AccountOrderApiController when it gets notified
			for (var recordUser : record.getUsers()) {
				if (Objects.equals(recordUser.getUser().getUserType(), SupportedUserTypeService.getActiveDirectoryUserType())) {
					var userExists = person.getUsers().stream().anyMatch(pu ->
						Objects.equals(pu.getUser().getUserType(), recordUser.getUser().getUserType())
						&& Objects.equals(pu.getUser().getMaster(), recordUser.getUser().getMaster())
						&& Objects.equals(pu.getUser().getMasterId(), recordUser.getUser().getMasterId()));
					
					if (!userExists) {
						// lookup matching account order
						var matchingAccountOrders = accountOrderService.findOrder(SupportedUserTypeService.getActiveDirectoryUserType(),AccountOrderType.CREATE, AccountOrderStatus.CREATED, recordUser.getUser().getUserId());						
						if (matchingAccountOrders != null && matchingAccountOrders.size() > 0) {
							recordUser.getUser().setEmployeeId(matchingAccountOrders.get(0).getEmployeeId());
						}
					}
				}
			}

			// perform normal patchCollection check
			if (this.<PersonUserMapping>patchCollection(person, record, Person.class.getMethod("getUsers"), Person.class.getMethod("setUsers", List.class))) {
				changes = true;
			}
		}
		
		if (record.getAffiliations() != null) {
			if (this.<Affiliation>patchCollection(person, record, Person.class.getMethod("getAffiliations"), Person.class.getMethod("setAffiliations", List.class))) {
				changes = true;
			}
		}
		
		if (record.getPhones() != null) {
			if (this.<PersonPhoneMapping>patchCollection(person, record, Person.class.getMethod("getPhones"), Person.class.getMethod("setPhones", List.class))) {
				changes = true;
			}
		}
		
		if (record.getResidencePostAddress() != null) {
			if (patchResidencePostAddress(person, record)) {
				changes = true;
			}
		}

		if (record.getRegisteredPostAddress() != null) {
			if (patchRegisteredPostAddress(person, record)) {
				changes = true;
			}
		}

		// if there are changes, flip any delete flag
		if (changes) {
			person.setDeleted(false);
		}

		return changes;
	}
	
	private boolean patchRegisteredPostAddress(Person person, Person record) {
		boolean changes = false;

		if (record.getRegisteredPostAddress() != null) {
			if (person.getRegisteredPostAddress() == null) {
				person.setRegisteredPostAddress(record.getRegisteredPostAddress());
				changes = true;
			}
			else {
				Post personPost = person.getRegisteredPostAddress();
				Post recordPost = record.getRegisteredPostAddress();

				if (patchPost(personPost, recordPost)) {
					changes = true;
				}
			}
		}

		return changes;
	}

	private boolean patchResidencePostAddress(Person person, Person record) {
		boolean changes = false;

		if (record.getResidencePostAddress() != null) {
			if (person.getResidencePostAddress() == null) {
				person.setResidencePostAddress(record.getResidencePostAddress());
				changes = true;
			}
			else {
				Post personPost = person.getResidencePostAddress();
				Post recordPost = record.getResidencePostAddress();

				if (patchPost(personPost, recordPost)) {
					changes = true;
				}
			}
		}

		return changes;

	}

	private boolean patchPost(Post personPost, Post recordPost) {
		boolean changes = false;

		// TODO: should we allow external APIs to take control of a POST object? If is likely controlled by the CPR integration,
		//       so the current master should be SOFD, right?
		if (recordPost.getMaster() != null && !Objects.equals(personPost.getMaster(), recordPost.getMaster())) {
			personPost.setMaster(recordPost.getMaster());
			changes = true;
		}

		if (recordPost.getMasterId() != null && !Objects.equals(personPost.getMasterId(), recordPost.getMasterId())) {
			personPost.setMasterId(recordPost.getMasterId());
			changes = true;
		}
		
		if (recordPost.getCity() != null && !Objects.equals(personPost.getCity(), recordPost.getCity())) {
			personPost.setCity(recordPost.getCity());
			changes = true;
		}
		
		if (recordPost.getCountry() != null && !Objects.equals(personPost.getCountry(), recordPost.getCountry())) {
			personPost.setCountry(recordPost.getCountry());
			changes = true;
		}
		
		if (recordPost.getLocalname() != null && !Objects.equals(personPost.getLocalname(), recordPost.getLocalname())) {
			personPost.setLocalname(recordPost.getLocalname());
			changes = true;
		}
		
		if (recordPost.getPostalCode() != null && !Objects.equals(personPost.getPostalCode(), recordPost.getPostalCode())) {
			personPost.setPostalCode(recordPost.getPostalCode());
			changes = true;
		}
		
		if (recordPost.getStreet() != null && !Objects.equals(personPost.getStreet(), recordPost.getStreet())) {
			personPost.setStreet(recordPost.getStreet());
			changes = true;
		}
		
		if (recordPost.isAddressProtected() != personPost.isAddressProtected()) {
			personPost.setAddressProtected(recordPost.isAddressProtected());
			changes = true;
		}

		return changes;
	}

	@SuppressWarnings("unchecked")
	private <T extends MappedEntity> boolean patchCollection(Person person, Person record, Method getCollectionMethod, Method setCollectionMethod) throws Exception {
		boolean changes = false;
		
		Collection<T> recordCollection = (Collection<T>) getCollectionMethod.invoke(record);
		Collection<T> personCollection = (Collection<T>) getCollectionMethod.invoke(person);

		// record has no entries, person does
		if (recordCollection == null || recordCollection.size() == 0) {
			if (personCollection != null && personCollection.size() > 0) {
				for (Iterator<T> iterator = personCollection.iterator(); iterator.hasNext();) {
					iterator.next();
					iterator.remove();
				}

				changes = true;
			}
		}
		else { // record has entries in all of the below cases
			// existing person does not have any entries
			if (personCollection == null || personCollection.size() == 0) {
				if (personCollection == null) {
					setCollectionMethod.invoke(person, new ArrayList<T>());
				}

				for (T recordEntry : recordCollection) {
					checkForADUserWithSameUserId(recordEntry);
					personCollection.add(recordEntry);
				}
				
				changes = true;
			}
			else {
				// both have entries, the big comparison case

				// to add or update
				for (T recordEntry : recordCollection) {
					boolean found = false;
					
					for (T personEntry : personCollection) {
						MasteredEntity recordMasteredEntity = recordEntry.getEntity();
						MasteredEntity personMasteredEntity = personEntry.getEntity();

						if (Objects.equals(personMasteredEntity.getMaster(), recordMasteredEntity.getMaster()) &&
							Objects.equals(personMasteredEntity.getMasterId(), recordMasteredEntity.getMasterId())) {

							if (personMasteredEntity instanceof Phone) {
								if (patchPhoneEntityFields((Phone) personMasteredEntity, (Phone) recordMasteredEntity)) {
									changes = true;
								}
							}
							else if (personMasteredEntity instanceof User) {
								if (patchUserEntityFields(person, (User) personMasteredEntity, (User) recordMasteredEntity)) {
									changes = true;
								}
							}
							else if (personMasteredEntity instanceof Affiliation) {
								if (patchAffiliationEntityFields((Affiliation) personMasteredEntity, (Affiliation) recordMasteredEntity)) {
									changes = true;
								}
							}
							
							found = true;
							break;
						}
					}

					// add if it does not exist
					if (!found) {
						checkForADUserWithSameUserId(recordEntry);
						
						personCollection.add(recordEntry);
						changes = true;
					}
				}
				
				// to remove
				for (Iterator<T> iterator = personCollection.iterator(); iterator.hasNext();) {
					T personEntry = iterator.next();
					boolean found = false;

					for (T recordEntry : recordCollection) {
						MasteredEntity recordMasteredEntity = recordEntry.getEntity();
						MasteredEntity personMasteredEntity = personEntry.getEntity();

						if (Objects.equals(personMasteredEntity.getMaster(), recordMasteredEntity.getMaster()) &&
							Objects.equals(personMasteredEntity.getMasterId(), recordMasteredEntity.getMasterId())) {
							
							found = true;
							break;
						}
					}
					
					// add if it does not exist
					if (!found) {
						iterator.remove();
						changes = true;
					}
				}
			}
		}

		return changes;
	}

	private boolean patchAffiliationEntityFields(Affiliation personEntry, Affiliation recordEntry) {
		boolean changes = false;

		if (recordEntry.getAffiliationType() != null && !Objects.equals(personEntry.getAffiliationType(), recordEntry.getAffiliationType())) {
			personEntry.setAffiliationType(recordEntry.getAffiliationType());
			changes = true;
		}

		if (recordEntry.getEmployeeId() != null && !Objects.equals(personEntry.getEmployeeId(), recordEntry.getEmployeeId())) {
			personEntry.setEmployeeId(recordEntry.getEmployeeId());
			changes = true;
		}

		if (recordEntry.getEmploymentTerms() != null && !Objects.equals(personEntry.getEmploymentTerms(), recordEntry.getEmploymentTerms())) {
			personEntry.setEmploymentTerms(recordEntry.getEmploymentTerms());
			changes = true;
		}
		
		if (recordEntry.getEmploymentTermsText() != null && !Objects.equals(personEntry.getEmploymentTermsText(), recordEntry.getEmploymentTermsText())) {
			personEntry.setEmploymentTermsText(recordEntry.getEmploymentTermsText());
			changes = true;
		}

		if (recordEntry.getLocalExtensions() != null && !Objects.equals(personEntry.getLocalExtensions(), recordEntry.getLocalExtensions())) {
			personEntry.setLocalExtensions(recordEntry.getLocalExtensions());
			changes = true;
		}
		
		if (recordEntry.getPayGrade() != null && !Objects.equals(personEntry.getPayGrade(), recordEntry.getPayGrade())) {
			personEntry.setPayGrade(recordEntry.getPayGrade());
			changes = true;
		}

		if (recordEntry.getPayGradeText() != null && !Objects.equals(personEntry.getPayGradeText(), recordEntry.getPayGradeText())) {
			personEntry.setPayGradeText(recordEntry.getPayGradeText());
			changes = true;
		}

		if (recordEntry.getSuperiorLevel() != null && !Objects.equals(personEntry.getSuperiorLevel(), recordEntry.getSuperiorLevel())) {
			personEntry.setSuperiorLevel(recordEntry.getSuperiorLevel());
			changes = true;
		}

		if (recordEntry.getSubordinateLevel() != null && !Objects.equals(personEntry.getSubordinateLevel(), recordEntry.getSubordinateLevel())) {
			personEntry.setSubordinateLevel(recordEntry.getSubordinateLevel());
			changes = true;
		}

		if (recordEntry.getWageStep() != null && !Objects.equals(personEntry.getWageStep(), recordEntry.getWageStep())) {
			personEntry.setWageStep(recordEntry.getWageStep());
			changes = true;
		}

		if (recordEntry.getPositionId() != null && !Objects.equals(personEntry.getPositionId(), recordEntry.getPositionId())) {
			personEntry.setPositionId(recordEntry.getPositionId());
			changes = true;
		}
		
		if (recordEntry.getPositionName() != null && !Objects.equals(personEntry.getPositionName(), recordEntry.getPositionName())) {
			personEntry.setPositionName((StringUtils.hasLength(recordEntry.getPositionName())) ? recordEntry.getPositionName().trim() : "Ukendt");
			changes = true;
		}

		if (recordEntry.getPositionShort() != null && !Objects.equals(personEntry.getPositionShort(), recordEntry.getPositionShort())) {
			personEntry.setPositionShort(recordEntry.getPositionShort());
			changes = true;
		}

		if (recordEntry.getPositionTypeId() != null && !Objects.equals(personEntry.getPositionTypeId(), recordEntry.getPositionTypeId())) {
			personEntry.setPositionTypeId(recordEntry.getPositionTypeId());
			changes = true;
		}
		
		if (recordEntry.getPositionTypeName() != null && !Objects.equals(personEntry.getPositionTypeName(), recordEntry.getPositionTypeName())) {
			personEntry.setPositionTypeName(recordEntry.getPositionTypeName());
			changes = true;
		}
		
		if (recordEntry.getStartDate() != null && !Objects.equals(toLocalDate(personEntry.getStartDate()), toLocalDate(recordEntry.getStartDate()))) {
			personEntry.setStartDate(recordEntry.getStartDate());
			changes = true;
		}

		if (recordEntry.getStopDate() != null && !Objects.equals(toLocalDate(personEntry.getStopDate()), toLocalDate(recordEntry.getStopDate()))) {
			personEntry.setStopDate(recordEntry.getStopDate());
			changes = true;
		}
		else if (recordEntry.getStopDate() == null && personEntry.getStopDate() != null) {
			personEntry.setStopDate(null);
			changes = true;
		}

		if (recordEntry.getWorkingHoursDenominator() != null && !Objects.equals(personEntry.getWorkingHoursDenominator(), recordEntry.getWorkingHoursDenominator())) {
			personEntry.setWorkingHoursDenominator(recordEntry.getWorkingHoursDenominator());
			changes = true;
		}
		
		if (recordEntry.getWorkingHoursNumerator() != null && !Objects.equals(personEntry.getWorkingHoursNumerator(), recordEntry.getWorkingHoursNumerator())) {
			personEntry.setWorkingHoursNumerator(recordEntry.getWorkingHoursNumerator());
			changes = true;
		}

		if (personEntry.isDeleted() != recordEntry.isDeleted()) {
			personEntry.setDeleted(recordEntry.isDeleted());
			changes = true;
		}

		if (recordEntry.getFunctions() != null) {
			if (recordEntry.getFunctions().size() > 0) {
				if (personEntry.getFunctions() == null || personEntry.getFunctions().size() == 0) {
					if (personEntry.getFunctions() == null) {
						personEntry.setFunctions(new ArrayList<>());
					}
					
					for (AffiliationFunctionMapping function : recordEntry.getFunctions()) {
						personEntry.getFunctions().add(function);
					}
					
					changes = true;
				}
				else {
					// complex update case
					
					// add
					for (AffiliationFunctionMapping recordFunction : recordEntry.getFunctions()) {
						boolean found = false;

						for (AffiliationFunctionMapping personFunction : personEntry.getFunctions()) {
							if (Objects.equals(personFunction.getFunction(), recordFunction.getFunction())) {
								found = true;
								break;
							}
						}

						if (!found) {
							personEntry.getFunctions().add(recordFunction);
							changes = true;
						}
					}
					
					// remove
					for (Iterator<AffiliationFunctionMapping> iterator = personEntry.getFunctions().iterator(); iterator.hasNext();) {
						AffiliationFunctionMapping personFunction = iterator.next();
						boolean found = false;

						for (AffiliationFunctionMapping recordFunction : recordEntry.getFunctions()) {
							if (Objects.equals(personFunction.getFunction(), recordFunction.getFunction())) {
								found = true;
								break;
							}
						}
						
						if (!found) {
							iterator.remove();
							changes = true;
						}
					}
				}
			}
			else {
				// supplied empty set, so remove all
				if (personEntry.getFunctions() != null && personEntry.getFunctions().size() > 0) {
					for (Iterator<AffiliationFunctionMapping> iterator = personEntry.getFunctions().iterator(); iterator.hasNext();) {
						iterator.next();
						iterator.remove();
					}

					changes = true;
				}
			}
		}

		if (recordEntry.getOrgUnit() != null) {
			if (!recordEntry.getOrgUnit().getUuid().equals(personEntry.getOrgUnit().getUuid())) {
				personEntry.setOrgUnit(recordEntry.getOrgUnit());
				changes = true;				
			}
		}

		// TODO: dont know if we should do this task says to only support it as read-only in v2 api
//		if (recordEntry.getAlternativeOrgUnit() != null) {
//			if (!recordEntry.getAlternativeOrgUnit().getUuid().equals(personEntry.getAlternativeOrgUnit().getUuid())) {
//				personEntry.setAlternativeOrgUnit(recordEntry.getAlternativeOrgUnit());
//				changes = true;
//			}
//		}

		// TODO: should probably not support changing this
		if (recordEntry.getUuid() != null && !Objects.equals(personEntry.getUuid(), recordEntry.getUuid())) {
			personEntry.setUuid(recordEntry.getUuid());
			changes = true;
		}

		return changes;
	}
	
	private boolean patchPhoneEntityFields(Phone personEntry, Phone recordEntry) {
		boolean changes = false;

		// note that patching cannot be used for null'ing fields, only setting or updating them

		if (recordEntry.getFunctionType() != null && !Objects.equals(personEntry.getFunctionType(), recordEntry.getFunctionType())) {
			personEntry.setFunctionType(recordEntry.getFunctionType());
			changes = true;
		}

		if (recordEntry.getPhoneNumber() != null && !Objects.equals(personEntry.getPhoneNumber(), recordEntry.getPhoneNumber())) {
			personEntry.setPhoneNumber(recordEntry.getPhoneNumber());
			changes = true;
		}

		if (recordEntry.getPhoneType() != null && !Objects.equals(personEntry.getPhoneType(), recordEntry.getPhoneType())) {
			personEntry.setPhoneType(recordEntry.getPhoneType());
			changes = true;
		}

		if (recordEntry.getVisibility() != null && !Objects.equals(personEntry.getVisibility(), recordEntry.getVisibility())) {
			personEntry.setVisibility(recordEntry.getVisibility());
			changes = true;
		}
		
		return changes;
	}

	private boolean patchUserEntityFields(Person person, User personUser, User recordUser) {
		boolean changes = false;
		
		// note that patching cannot be used for null'ing fields, only setting or updating them
		
		// never allow setting/updating employeeId on vikXXXX users, as this will not work well over time
		if (!UserService.isSubstituteUser(personUser)) {
			if (recordUser.getEmployeeId() != null && !Objects.equals(personUser.getEmployeeId(), recordUser.getEmployeeId())) {
				// we only allow changing the employeeId on AD accounts if the version in SOFD Core is NULL, because
				// SOFD Core should be the master of this field (but initial load from AD is okay)
				// If EmployeeAssociation is not enabled in SOFD, changing the attribute is also allowed
				if (!StringUtils.hasLength(personUser.getEmployeeId()) || !SupportedUserTypeService.isActiveDirectory(personUser.getUserType()) || !sofdConfiguration.getIntegrations().getOpus().isEnableActiveDirectoryEmployeeIdAssociation()) {
					personUser.setEmployeeId(recordUser.getEmployeeId());
					changes = true;
				}
			}
		}

		if (recordUser.getLocalExtensions() != null && !Objects.equals(personUser.getLocalExtensions(), recordUser.getLocalExtensions())) {
			personUser.setLocalExtensions(recordUser.getLocalExtensions());
			changes = true;
		}
		
		if (recordUser.getUuid() != null && !Objects.equals(personUser.getUuid(), recordUser.getUuid())) {
			personUser.setUuid(recordUser.getUuid());
			changes = true;
		}

		if (recordUser.getUserId() != null && !Objects.equals(personUser.getUserId(), recordUser.getUserId())) {
			personUser.setUserId(recordUser.getUserId());
			changes = true;
		}

		if (recordUser.getUserType() != null && !Objects.equals(personUser.getUserType(), recordUser.getUserType())) {
			personUser.setUserType(recordUser.getUserType());
			changes = true;
		}
		
		// TODO: these two boolean fields are updated in a round-about way - we use a Boolean (allowing null for patching), but update
		//       a boolean field (to ensure non-null) on the User object

		if (recordUser.getTSubstituteAccount() != null && !Objects.equals(personUser.isSubstituteAccount(), recordUser.getTSubstituteAccount())) {
			personUser.setSubstituteAccount(recordUser.getTSubstituteAccount());
			changes = true;
		}

		if (recordUser.getTDisabled() != null && !Objects.equals(personUser.isDisabled(), recordUser.getTDisabled())) {
			personUser.setDisabled(recordUser.getTDisabled());
			changes = true;
		}

		if (patchActiveDirectoryFields(person, personUser, recordUser)) {
			changes = true;
		}
		
		return changes;
	}
	
	private LocalDate toLocalDate(Date date) {
		if (date == null) {
			return null;
		}

		// TODO: hack, remove once we no longer need it (look in LocalDateAttributeConverter for reason)
		if (date instanceof java.sql.Date) {
			return ((java.sql.Date) date).toLocalDate();
		}
		
	    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}

	// this is called when a PATCH operation on a Person adds an ACTIVE_DIRECTORY user object. We need to ensure
	// that this User object does not exist on other persons, to avoid duplicate issues in our synchronization with
	// other external systems. This removes the User object from the other Person
	private <T extends MappedEntity> void checkForADUserWithSameUserId(T recordEntry) {
		try {
			MasteredEntity recordMasteredEntity = recordEntry.getEntity();
			
			if (recordMasteredEntity instanceof User &&
				Objects.equals(((User) recordMasteredEntity).getUserType(), SupportedUserTypeService.getActiveDirectoryUserType())) {
	
				String userId = ((User) recordMasteredEntity).getUserId();
				List<Person> personsWithSameAD = personService.findByUserTypeAndUserId(SupportedUserTypeService.getActiveDirectoryUserType(), userId);
				
				for (Person personWithSameAD : personsWithSameAD) {
					log.info("Deleting user with userId " + userId + " of type Active Directory from person with uuid " + personWithSameAD.getUuid());

					personWithSameAD.getUsers().removeIf(u -> Objects.equals(u.getUser().getUserId(), userId) && SupportedUserTypeService.isActiveDirectory(u.getUser().getUserType()));
					personService.save(personWithSameAD);
				}
			}
		}
		catch (Exception ex) {
			log.error("Failed to remove '" + recordEntry.getEntity().getMasterId() + "' from other person", ex);
		}
	}

	// same as above, but this is called when creating a new Person
	private void checkForADUserWithSameUserId(Person person) {
		try {
			for (User user : PersonService.getUsers(person)) {
				if (SupportedUserTypeService.isActiveDirectory(user.getUserType())) {					
					List<Person> personsWithSameAD = personService.findByUserTypeAndUserId(SupportedUserTypeService.getActiveDirectoryUserType(), user.getUserId());
	
					for (Person personWithSameAD : personsWithSameAD) {
						if (!Objects.equals(personWithSameAD.getUuid(), person.getUuid())) {
							log.info("Deleting user with userId " + user.getUserId() + " of type Active Directory from person with uuid " + personWithSameAD.getUuid());
		
							personWithSameAD.getUsers().removeIf(u -> Objects.equals(u.getUser().getUserId(), user.getUserId()) && SupportedUserTypeService.isActiveDirectory(u.getUser().getUserType()));				
							personService.save(personWithSameAD);
						}
					}
				}
			}
		}
		catch (Exception ex) {
			log.error("Failed to remove duplicate users based on source '" + PersonService.maskCpr(person.getCpr()) + "' from other person", ex);
		}
	}
	
	private boolean patchActiveDirectoryFields(Person person, User user, User userRecord) {
		boolean changes = false;

		if (!SupportedUserTypeService.isActiveDirectory(user.getUserType()) && !SupportedUserTypeService.isActiveDirectorySchool(user.getUserType())) {
			return changes;
		}
		
		if (!SupportedUserTypeService.isActiveDirectory(userRecord.getUserType()) && !SupportedUserTypeService.isActiveDirectorySchool(userRecord.getUserType())) {
			return changes;
		}

		if (!Objects.equals(userRecord.getMaster(), user.getMaster()) || !Objects.equals(userRecord.getMasterId(), user.getMasterId())) {
			return changes;
		}

		ActiveDirectoryDetails details = user.getActiveDirectoryDetails();
		if (details == null) {
			details = new ActiveDirectoryDetails();
			details.setUser(user);
			details.setUserType(user.getUserType());

			user.setActiveDirectoryDetails(details);
			changes = true;
		}

		// no reason to set change = true here - either we are in a very strange migration case (and then the migration should
		// make sure this is set), or details was NULL above, so kombitUuid will also be null
		if (!StringUtils.hasLength(details.getKombitUuid())) {
			if (sofdConfiguration.getIntegrations().getOs2sync().isUseObjectGuidAsKombitUuid()) {
				// use object guid from AD as uuid in FK Org etc.
				details.setKombitUuid(user.getMasterId());
			}
			else {
				// generate a uuid based on cvr+cpr+user_id+userType. This is to prevent uuid changes for
				// municipalites that for some reason deletes and recreates user objects
				var seed = sofdConfiguration.getCustomer().getCvr() + person.getCpr() + user.getUserId() + user.getUserType();
				details.setKombitUuid(UUID.nameUUIDFromBytes(seed.toLowerCase().getBytes()).toString());
			}
		}
		
		// TODO: these booleans cannot be null, so we cannot avoid patching them - that is an issue
		if (userRecord.getActiveDirectoryDetails().isPasswordLocked() && details.isPasswordLocked() == false) {
			changes = true;
			details.setPasswordLocked(true);
			details.setPasswordLockedDate(LocalDate.now());
		}
		else if (!userRecord.getActiveDirectoryDetails().isPasswordLocked() && details.isPasswordLocked() == true) {
			changes = true;
			details.setPasswordLocked(false);
			details.setPasswordLockedDate(null);
		}

		if (userRecord.getActiveDirectoryDetails().getWhenCreated() != null && !Objects.equals(userRecord.getActiveDirectoryDetails().getWhenCreated(), details.getWhenCreated())) {
			changes = true;
			details.setWhenCreated(userRecord.getActiveDirectoryDetails().getWhenCreated());
		}

		if (userRecord.getActiveDirectoryDetails().getAccountExpireDate() != null) {
			// the backend can deliver a custom 9999-12-31 value, which means NULL, so we can support patch'ing to null :)
			LocalDate newValue = userRecord.getActiveDirectoryDetails().getAccountExpireDate();
			if (Objects.equals(year9999, newValue)) {
				newValue = null;
			}

			if (!Objects.equals(newValue, details.getAccountExpireDate())) {
				changes = true;
				details.setAccountExpireDate(userRecord.getActiveDirectoryDetails().getAccountExpireDate());
			}
		}

		if (userRecord.getActiveDirectoryDetails().getPasswordExpireDate() != null && !Objects.equals(userRecord.getActiveDirectoryDetails().getPasswordExpireDate(), details.getPasswordExpireDate())) {
			// the backend can deliver a custom 9999-12-31 value, which means NULL, so we can support patch'ing to null :)
			LocalDate newValue = userRecord.getActiveDirectoryDetails().getPasswordExpireDate();
			if (Objects.equals(year9999, newValue)) {
				newValue = null;
			}

			if (!Objects.equals(newValue, details.getPasswordExpireDate())) {
				changes = true;
				details.setPasswordExpireDate(userRecord.getActiveDirectoryDetails().getPasswordExpireDate());
			}
		}

		if (userRecord.getActiveDirectoryDetails().getUpn() != null && !Objects.equals(userRecord.getActiveDirectoryDetails().getUpn(), details.getUpn())) {
			changes = true;
			details.setUpn(userRecord.getActiveDirectoryDetails().getUpn());
		}

		if (userRecord.getActiveDirectoryDetails().getTitle() != null && !Objects.equals(userRecord.getActiveDirectoryDetails().getTitle(), details.getTitle())) {
			changes = true;
			details.setTitle(userRecord.getActiveDirectoryDetails().getTitle());
		}

		return changes;
	}
}
