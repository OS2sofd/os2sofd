package dk.digitalidentity.sofd.telephony.api;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.security.RequireApiWriteAccess;
import dk.digitalidentity.sofd.service.FunctionTypeService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.telephony.TelephonyConstants;
import dk.digitalidentity.sofd.telephony.api.dto.TelephonyPhoneDTO;
import dk.digitalidentity.sofd.telephony.dao.TelephonyPhoneDao;
import dk.digitalidentity.sofd.telephony.dao.model.TelephonyPhone;
import dk.digitalidentity.sofd.telephony.dao.model.TelephonyPhoneOrgUnit;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequireApiWriteAccess
public class TelephonyApiController {
	public static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	@Autowired
	private TelephonyPhoneDao telephonyPhoneDao;

	@Autowired
	private FunctionTypeService functionTypeService;

	@Autowired
	private PersonService personService;

	@Autowired
	private OrgUnitService orgUnitService;

//	List all existing telephone numbers stored in the telephone module (including all the related data, like type, number, associcated orgunit/person, etc)

	@GetMapping({"/api/telephony/", "/api/telephony" })
	public ResponseEntity<?> listAllNumbers() {
		List<TelephonyPhoneDTO> list = telephonyPhoneDao.findAll().stream().map(TelephonyPhoneDTO::new).toList();

		return new ResponseEntity<>(list, HttpStatus.OK);
	}

//	Create a new (un-associated) phone number (so basic data like type and number)

	@PostMapping("/api/telephony/")
	public ResponseEntity<?> createNewNumber(@RequestBody TelephonyPhoneDTO body) {
		TelephonyPhone phone = new TelephonyPhone();
		phone.setMaster(TelephonyConstants.TELEPHONY_MASTER);
		phone.setMasterId(UUID.randomUUID().toString());
		phone.setPhoneNumber(body.getPhoneNumber());
		phone.setVendor(body.getVendor());
		phone.setAccountNumber(body.getAccountNumber());
		phone.setEan(body.getEan());
		phone.setPhoneType(body.getPhoneType());
		phone.setVisibility(body.getVisibility());
		phone.setFunctionType(functionTypeService.findAll().stream().filter(ft -> ft.getName().equals(body.getFunctionType())).findAny().orElse(null));

		phone.setSubscriptionType(body.getSubscriptionType());
		phone.setNotes(body.getNotes());

		telephonyPhoneDao.save(phone);

		return new ResponseEntity<>(new TelephonyPhoneDTO(phone), HttpStatus.OK);
	}

//	Assign or remove a person from a phonenumber
	@PostMapping("/api/telephony/{phoneId}/person")
	public ResponseEntity<?> assignPerson(@PathVariable("phoneId") long phoneId, @RequestBody String personUuid) {
		TelephonyPhone phone = telephonyPhoneDao.findById(phoneId);
		if (phone == null) {
			log.warn("Phone not found for id: " + phoneId);
			return ResponseEntity.badRequest().body("Phone not found for id: " + phoneId);
		}

		Person person = personService.getByUuid(personUuid);
		if (person == null) {
			log.warn("Person not found for uuid: " + personUuid);
			return ResponseEntity.badRequest().body("Person not found for uuid: " + personUuid);
		}
		else {
			phone.setPersonName(PersonService.getName(person));
			phone.setPersonUuid(person.getUuid());
		}

		telephonyPhoneDao.save(phone);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@DeleteMapping("/api/telephony/{phoneId}/person")
	public ResponseEntity<?> unassignPerson(@PathVariable("phoneId") long phoneId) {
		TelephonyPhone phone = telephonyPhoneDao.findById(phoneId);
		if (phone == null) {
			log.warn("Phone not found for id: " + phoneId);
			return ResponseEntity.badRequest().body("Phone not found for id: " + phoneId);
		}

		phone.setPersonName(null);
		phone.setPersonUuid(null);

		telephonyPhoneDao.save(phone);

		return new ResponseEntity<>(HttpStatus.OK);
	}
	
//	Assign or remove an Orgunit from a phonenumber

	@PostMapping("/api/telephony/{phoneId}/orgunit")
	public ResponseEntity<?> assignOrgUnit(@PathVariable("phoneId") long phoneId, @RequestBody String orgUnitUuid) {
		TelephonyPhone phone = telephonyPhoneDao.findById(phoneId);
		if (phone == null) {
			log.warn("Phone not found for id: " + phoneId);
			return ResponseEntity.badRequest().body("Phone not found for id: " + phoneId);
		}

		OrgUnit orgUnit = orgUnitService.getByUuid(orgUnitUuid);
		if (orgUnit == null) {
			log.warn("OrgUnit for uuid: " + orgUnitUuid + " not found");
			return ResponseEntity.badRequest().body("OrgUnit for uuid: " + orgUnitUuid + " not found");
		}
		
		if (phone.getOrgUnits() != null && phone.getOrgUnits().stream().anyMatch(mapping -> Objects.equals(mapping.getOrgunitUuid(), orgUnit.getUuid()))) {
			log.warn("OrgUnit for uuid: " + orgUnitUuid + " already assigned");
			return new ResponseEntity<>(HttpStatus.OK);
		}

		TelephonyPhoneOrgUnit tp = new TelephonyPhoneOrgUnit();
		tp.setOrgunitUuid(orgUnit.getUuid());
		tp.setOrgunitName(orgUnit.getName());
		tp.setTelephonyPhone(phone);

		phone.getOrgUnits().add(tp);

		telephonyPhoneDao.save(phone);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@DeleteMapping("/api/telephony/{phoneId}/orgunit/{orgUnitUuid}")
	public ResponseEntity<?> unassignOrgUnit(@PathVariable("phoneId") long phoneId, @PathVariable("orgUnitUuid") String orgUnitUuid) {
		TelephonyPhone phone = telephonyPhoneDao.findById(phoneId);
		if (phone == null) {
			log.warn("Phone not found for id: " + phoneId);
			return ResponseEntity.badRequest().body("Phone not found for id: " + phoneId);
		}

		OrgUnit orgUnit = orgUnitService.getByUuid(orgUnitUuid);
		if (orgUnit == null) {
			log.warn("OrgUnit for uuid: " + orgUnitUuid + " not found");
			return ResponseEntity.badRequest().body("OrgUnit for uuid: " + orgUnitUuid + " not found");
		}

		if (phone.getOrgUnits() == null) {
			log.warn("There are no OrgUnits assigned to given Phone");
			return ResponseEntity.badRequest().body("There are no OrgUnits assigned to given Phone");
		}

		if (phone.getOrgUnits() != null && phone.getOrgUnits().stream().noneMatch(mapping -> Objects.equals(mapping.getOrgunitUuid(), orgUnit.getUuid()))) {
			log.warn("OrgUnit for uuid: " + orgUnitUuid + " was not assigned to phone: " + phoneId);
			return ResponseEntity.badRequest().body("OrgUnit for uuid: " + orgUnitUuid + " was not assigned to phone: " + phoneId);
		}

		// remove
		for (Iterator<TelephonyPhoneOrgUnit> iterator = phone.getOrgUnits().iterator(); iterator.hasNext();) {
			TelephonyPhoneOrgUnit telephonyPhoneOrgUnit = iterator.next();
			
			if (Objects.equals(telephonyPhoneOrgUnit.getOrgunitUuid(), orgUnit.getUuid())) {
				iterator.remove();
			}
		}

		telephonyPhoneDao.save(phone);

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
