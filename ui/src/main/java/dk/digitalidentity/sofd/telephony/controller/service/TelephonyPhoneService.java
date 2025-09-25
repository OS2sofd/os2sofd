package dk.digitalidentity.sofd.telephony.controller.service;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.Phone;
import dk.digitalidentity.sofd.dao.model.Setting;
import dk.digitalidentity.sofd.dao.model.enums.CustomerSetting;
import dk.digitalidentity.sofd.dao.model.mapping.OrgUnitPhoneMapping;
import dk.digitalidentity.sofd.dao.model.mapping.PersonPhoneMapping;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.SettingService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.telephony.TelephonyConstants;
import dk.digitalidentity.sofd.telephony.controller.rest.dto.TelephonyHistoryDTO;
import dk.digitalidentity.sofd.telephony.dao.TelephonyPhoneDao;
import dk.digitalidentity.sofd.telephony.dao.model.TelephonyHistoryView;
import dk.digitalidentity.sofd.telephony.dao.model.TelephonyPhone;
import dk.digitalidentity.sofd.telephony.dao.model.TelephonyPhoneOrgUnit;

@Service
public class TelephonyPhoneService {

	@Autowired
	private TelephonyPhoneDao telephonyPhoneDao;

	@Autowired
	private SettingService settingService;
	
	@Autowired
	private PersonService personService;
	
	@Autowired
	private OrgUnitService orgUnitService;

	public List<TelephonyPhone> findAll() {
		return telephonyPhoneDao.findAll();
	}

	public List<TelephonyHistoryDTO> findHistoricPhoneHolders(String term) {
		List<TelephonyHistoryView> revisions = telephonyPhoneDao.findHistoricPhoneHoldersByPhoneNumber(term);

		List<TelephonyHistoryDTO> sortedList = revisions.stream()
				.map(t -> new TelephonyHistoryDTO(t))
				.sorted((a, b) -> a.getAssignedFrom().isAfter(b.getAssignedFrom()) ? 1 : -1)
				.collect(Collectors.toList());

		TelephonyHistoryDTO previous = null;
		for (TelephonyHistoryDTO view : sortedList) {
			if (previous != null) {
				previous.setAssignedTo(view.getAssignedFrom());
			}

			previous = view;
		}

		// remove empty entries (time-slots where the phone is not owned by anyone)
		sortedList = sortedList.stream()
				.filter(p -> StringUtils.hasLength(p.getPerson()))
				.collect(Collectors.toList());
		
		return sortedList;
	}
	
	@Transactional(rollbackFor = Exception.class)
	public long deltaSync() throws Exception {
		long count = 0;
		SecurityUtil.fakeLoginSession();

		Long maxRevision = telephonyPhoneDao.getMaxRev();
		if (maxRevision == null) {
			maxRevision = 0L;
		}

		Long currentRevisionNumber = 0L;
		Setting setting = settingService.getByKey(CustomerSetting.LAST_TELEPHONY_REVISION);
		if (setting != null) {
			currentRevisionNumber = Long.parseLong(setting.getValue());
		}
		else {
			setting = new Setting();
			setting.setKey(CustomerSetting.LAST_TELEPHONY_REVISION.toString());
		}

		// if there are changes, sync them
		if (currentRevisionNumber < maxRevision) {
			List<TelephonyPhone> modifiedPhones = telephonyPhoneDao.getChangesSince(currentRevisionNumber, maxRevision);
			
			for (TelephonyPhone telephonyPhone : modifiedPhones) {
				if (telephonyPhone.getOrgUnits() != null && telephonyPhone.getOrgUnits().size() > 0) {
					for (TelephonyPhoneOrgUnit orgUnit : telephonyPhone.getOrgUnits()) {
						if (updateOrgUnit(telephonyPhone, orgUnitService.getByUuid(orgUnit.getOrgunitUuid()))) {
							count++;
						}						
					}
				}
				
				if (StringUtils.hasLength(telephonyPhone.getPersonUuid())) {
					if (updatePerson(telephonyPhone, personService.getByUuid(telephonyPhone.getPersonUuid()))) {
						count++;
					}
				}
				else {
					// delete this phoneNumber from any person in SOFD
					var personsWithPhone = personService.getByPhoneMasterAndMasterId(telephonyPhone.getMaster(), telephonyPhone.getMasterId());
					var updated = false;
					for( var personWithPhone : personsWithPhone ) {
						personWithPhone.getPhones().removeIf(p -> p.getPhone().getMaster().equalsIgnoreCase(telephonyPhone.getMaster()) && p.getPhone().getMasterId().equalsIgnoreCase(telephonyPhone.getMasterId()));
						personService.save(personWithPhone);
						updated = true;
					}
					if( updated ) {
						count++;
					}
				}
			}
			
			// update setting
			setting.setValue(Long.toString(maxRevision));
			settingService.save(setting);
		}
		
		return count;
	}
	
	@Transactional(rollbackFor = Exception.class)
	public long fullSync() throws Exception {
		long count = 0;
		SecurityUtil.fakeLoginSession();

		// read from DB
		List<TelephonyPhone> allPhones = telephonyPhoneDao.findAll();
		List<Person> persons = personService.getActive();
		List<OrgUnit> orgUnits = orgUnitService.getAllActive();
		
		// convert to maps for fast lookup by key
		Map<String, TelephonyPhone> phoneMap = allPhones.stream().collect(Collectors.toMap(TelephonyPhone::getMasterId, Function.identity()));
		Map<String, Person> personMap = persons.stream().collect(Collectors.toMap(Person::getUuid, Function.identity()));
		Map<String, OrgUnit> orgUnitMap = orgUnits.stream().collect(Collectors.toMap(OrgUnit::getUuid, Function.identity()));

		// iterate over all phones, and find Persons/OrgUnits to add/update
		for (TelephonyPhone telephonyPhone : allPhones) {
			if (telephonyPhone.getOrgUnits() != null && telephonyPhone.getOrgUnits().size() > 0) {
				for (TelephonyPhoneOrgUnit orgUnit : telephonyPhone.getOrgUnits()) {
					if (updateOrgUnit(telephonyPhone, orgUnitMap.get(orgUnit.getOrgunitUuid()))) {
						count++;
					}					
				}
			}
			
			if (StringUtils.hasLength(telephonyPhone.getPersonUuid())) {
				if (updatePerson(telephonyPhone, personMap.get(telephonyPhone.getPersonUuid()))) {
					count++;
				}
			}			
		}
		
		// iterate over all Persons, and find phones to delete
		for (Person person : persons) {
			boolean changes = false;

			for (Iterator<PersonPhoneMapping> iterator = person.getPhones().iterator(); iterator.hasNext();) {
				PersonPhoneMapping mapping = iterator.next();
				Phone phone = mapping.getPhone();

				if (!phone.getMaster().equals(TelephonyConstants.TELEPHONY_MASTER)) {
					continue;
				}
				
				if (!phoneMap.containsKey(phone.getMasterId())) {
					// phone no longer exists
					iterator.remove();
					changes = true;
					count++;
				}
				else {
					// check if still assigned
					TelephonyPhone telephonyPhone = phoneMap.get(phone.getMasterId());
					if (!Objects.equals(telephonyPhone.getPersonUuid(), person.getUuid())) {
						iterator.remove();
						changes = true;
						count++;
					}
				}
			}
			
			if (changes) {
				personService.save(person);
			}
		}
		
		// iterate over all OrgUnits and find phones to delete
		for (OrgUnit orgUnit : orgUnits) {
			boolean changes = false;

			for (Iterator<OrgUnitPhoneMapping> iterator = orgUnit.getPhones().iterator(); iterator.hasNext();) {
				OrgUnitPhoneMapping phoneMapping = iterator.next();
				Phone phone = phoneMapping.getPhone();

				if (!phone.getMaster().equals(TelephonyConstants.TELEPHONY_MASTER)) {
					continue;
				}
				
				if (!phoneMap.containsKey(phone.getMasterId())) {
					// phone no longer exists
					iterator.remove();
					changes = true;
					count++;
				}
				else {
					// check if still assigned
					TelephonyPhone telephonyPhone = phoneMap.get(phone.getMasterId());
					
					boolean found = false;
					for (TelephonyPhoneOrgUnit telephonyPhoneOrgUnit : telephonyPhone.getOrgUnits()) {
						if (Objects.equals(telephonyPhoneOrgUnit.getOrgunitUuid(), orgUnit.getUuid())) {
							found = true;
							break;
						}
					}

					if (!found) {
						iterator.remove();
						changes = true;
						count++;
					}
				}
			}
			
			if (changes) {
				orgUnitService.save(orgUnit);
			}
		}

		return count;
	}
	
	private boolean updatePerson(TelephonyPhone telephonyPhone, Person person) {
		if (person == null) {
			return false;
		}

		Optional<Phone> oPhone = PersonService.getPhones(person).stream()
				.filter(p -> p.getMaster().equals(TelephonyConstants.TELEPHONY_MASTER) &&
							 p.getMasterId().equals(telephonyPhone.getMasterId()))
				.findFirst();

		boolean changes = false;

		if (oPhone.isPresent()) {
			Phone phone = oPhone.get();

			changes = compareAndUpdate(phone, telephonyPhone);
		}
		else {
			Phone phone = new Phone();
			phone.setFunctionType(telephonyPhone.getFunctionType());
			phone.setMaster(telephonyPhone.getMaster());
			phone.setMasterId(telephonyPhone.getMasterId());
			phone.setPhoneNumber(telephonyPhone.getPhoneNumber());
			phone.setPhoneType(telephonyPhone.getPhoneType());
			phone.setVisibility(telephonyPhone.getVisibility());
			
			PersonPhoneMapping mapping = new PersonPhoneMapping();
			mapping.setPerson(person);
			mapping.setPhone(phone);
			
			person.getPhones().add(mapping);
			
			changes = true;
		}

		if (changes) {
			personService.save(person);
		}
		
		return changes;
	}

	private boolean updateOrgUnit(TelephonyPhone telephonyPhone, OrgUnit orgUnit) throws Exception {
		if (orgUnit == null) {
			return false;
		}

		Optional<Phone> oPhone = OrgUnitService.getPhones(orgUnit).stream()
				.filter(p -> p.getMaster().equals(TelephonyConstants.TELEPHONY_MASTER) &&
							 p.getMasterId().equals(telephonyPhone.getMasterId()))
				.findFirst();

		boolean changes = false;

		if (oPhone.isPresent()) {
			Phone phone = oPhone.get();

			changes = compareAndUpdate(phone, telephonyPhone);
		}
		else {
			Phone phone = new Phone();
			phone.setFunctionType(telephonyPhone.getFunctionType());
			phone.setMaster(telephonyPhone.getMaster());
			phone.setMasterId(telephonyPhone.getMasterId());
			phone.setPhoneNumber(telephonyPhone.getPhoneNumber());
			phone.setPhoneType(telephonyPhone.getPhoneType());
			phone.setVisibility(telephonyPhone.getVisibility());
			
			OrgUnitPhoneMapping mapping = new OrgUnitPhoneMapping();
			mapping.setOrgUnit(orgUnit);
			mapping.setPhone(phone);
			
			orgUnit.getPhones().add(mapping);
			
			changes = true;
		}

		if (changes) {
			orgUnitService.save(orgUnit);
		}
		
		return changes;
	}
	
	private boolean compareAndUpdate(Phone phone, TelephonyPhone telephonyPhone) {
		boolean changes = false;

		if ((phone.getFunctionType() == null && telephonyPhone.getFunctionType() != null) || 
			(phone.getFunctionType() != null && telephonyPhone.getFunctionType() == null) ||
			(phone.getFunctionType() != null && telephonyPhone.getFunctionType() != null && phone.getFunctionType().getId() != telephonyPhone.getFunctionType().getId())) {
			phone.setFunctionType(telephonyPhone.getFunctionType());
			changes = true;
		}
		
		if (!Objects.equals(phone.getPhoneNumber(), telephonyPhone.getPhoneNumber())) {
			phone.setPhoneNumber(telephonyPhone.getPhoneNumber());
			changes = true;
		}
		
		if (!Objects.equals(phone.getPhoneType(), telephonyPhone.getPhoneType())) {
			phone.setPhoneType(telephonyPhone.getPhoneType());
			changes = true;
		}

		if (!Objects.equals(phone.getVisibility(),  telephonyPhone.getVisibility())) {
			phone.setVisibility(telephonyPhone.getVisibility());
			changes = true;
		}

		return changes;
	}
}
