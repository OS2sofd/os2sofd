package dk.digitalidentity.sofd.telephony.controller.rest;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import dk.digitalidentity.sofd.security.RequireReadAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.controller.mvc.datatables.dao.GridTelephonyPhoneDatatableDao;
import dk.digitalidentity.sofd.controller.mvc.datatables.dao.model.GridTelephonyPhone;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.FunctionType;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.security.RequireTelephonyAccess;
import dk.digitalidentity.sofd.service.FunctionTypeService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.telephony.TelephonyConstants;
import dk.digitalidentity.sofd.telephony.controller.mvc.dto.TelephonyPhoneDTO;
import dk.digitalidentity.sofd.telephony.controller.rest.dto.AutoCompleteResult;
import dk.digitalidentity.sofd.telephony.controller.rest.dto.TelephonyHistoryDTO;
import dk.digitalidentity.sofd.telephony.controller.rest.dto.ValueData;
import dk.digitalidentity.sofd.telephony.controller.service.TelephonyPhoneService;
import dk.digitalidentity.sofd.telephony.dao.TelephonyPhoneDao;
import dk.digitalidentity.sofd.telephony.dao.model.TelephonyPhone;
import dk.digitalidentity.sofd.telephony.dao.model.TelephonyPhoneOrgUnit;

@RequireTelephonyAccess
@RestController
public class TelephonyRestController {

	@Autowired
	private TelephonyPhoneDao telephonyPhoneDao;

	@Autowired
	private TelephonyPhoneService telephonyPhoneService;

	@Autowired
	private FunctionTypeService functionTypeService;

	@Autowired
	private PersonService personService;

	@Autowired
	private OrgUnitService orgUnitService;
	
	@Autowired
	private GridTelephonyPhoneDatatableDao gridTelephonyPhoneDatatableDao;

	@PostMapping("/rest/telephony/savePhone")
	@ResponseBody
	public ResponseEntity<HttpStatus> savePhone(@RequestBody TelephonyPhoneDTO phoneDTO) {
		// null allowed
		FunctionType functionType = functionTypeService.findById(phoneDTO.getFunctionType());

		TelephonyPhone phone = null;
		if (phoneDTO.getId() == 0) {
			phone = new TelephonyPhone();
			phone.setMaster(TelephonyConstants.TELEPHONY_MASTER);
			phone.setMasterId(UUID.randomUUID().toString());
			phone.setPhoneNumber(phoneDTO.getPhoneNumber());
			phone.setVendor(phoneDTO.getVendor());
			phone.setAccountNumber(phoneDTO.getAccountNumber());
			phone.setEan(phoneDTO.getEan());
			phone.setPhoneType(phoneDTO.getPhoneType());
			phone.setVisibility(phoneDTO.getVisibility());
			phone.setFunctionType(functionType);
			phone.setSubscriptionType(phoneDTO.getSubscriptionType());
			phone.setNotes(phoneDTO.getNotes());
		}
		else {
			phone = telephonyPhoneDao.findById(phoneDTO.getId());

			if (phone != null) {
				if (!phone.getMaster().equals(TelephonyConstants.TELEPHONY_MASTER)) {
					return new ResponseEntity<>(HttpStatus.FORBIDDEN);
				}

				phone.setPhoneNumber(phoneDTO.getPhoneNumber());
				phone.setVendor(phoneDTO.getVendor());
				phone.setAccountNumber(phoneDTO.getAccountNumber());
				phone.setEan(phoneDTO.getEan());
				phone.setPhoneType(phoneDTO.getPhoneType());
				phone.setVisibility(phoneDTO.getVisibility());
				phone.setFunctionType(functionType);
				phone.setSubscriptionType(phoneDTO.getSubscriptionType());
				phone.setNotes(phoneDTO.getNotes());
			}
			else {
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
			}
		}

		telephonyPhoneDao.save(phone);
		
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/rest/telephony/deletePhone")
	@ResponseBody
	public ResponseEntity<HttpStatus> deletePhone(@RequestHeader("id") long id) {
		TelephonyPhone phone = telephonyPhoneDao.findById(id);
		if (phone == null) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		if (!phone.getMaster().equals(TelephonyConstants.TELEPHONY_MASTER)) {
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}

		telephonyPhoneDao.deleteById(id);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping(value = "/rest/telephony/search/person")
	@RequireReadAccess
	@ResponseBody
	public ResponseEntity<?> searchPerson(@RequestParam("query") String term) {
		List<Person> persons = personService.findTop10ByName(term);

		List<ValueData> suggestions = new ArrayList<>();
		for (Person person : persons) {
			StringBuilder builder = new StringBuilder();
			builder.append(PersonService.getName(person));
			builder.append(" - ");
			builder.append(PersonService.maskCpr(person.getCpr()));

			Optional<Affiliation> primeAffiliation = person.getAffiliations().stream().filter(a -> a.isPrime()).findFirst();
			if (primeAffiliation.isPresent()) {
				builder.append(" (" + primeAffiliation.get().getCalculatedOrgUnit().getName() + ")"); // TODO: ok
			}
			
			ValueData vd = new ValueData();
			vd.setValue(builder.toString());
			vd.setData(person.getUuid());
			
			suggestions.add(vd);
		}

		AutoCompleteResult result = new AutoCompleteResult();
		result.setSuggestions(suggestions);

		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@PostMapping(value = "/rest/telephony/savePerson")
	@ResponseBody
	public ResponseEntity<HttpStatus> savePerson(@RequestHeader("phoneID") long phoneID, @RequestBody String personUuid) {
		TelephonyPhone phone = telephonyPhoneDao.findById(phoneID);
		if (phone == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		Person person = personService.getByUuid(personUuid);
		if (person == null) {
			phone.setPersonName(null);
			phone.setPersonUuid(null);
		}
		else {
			phone.setPersonName(PersonService.getName(person));
			phone.setPersonUuid(person.getUuid());
		}

		telephonyPhoneDao.save(phone);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/rest/telephony/list")
	public DataTablesOutput<GridTelephonyPhone> list(@Valid @RequestBody DataTablesInput input, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			DataTablesOutput<GridTelephonyPhone> error = new DataTablesOutput<>();
			error.setError(bindingResult.toString());

			return error;
		}

		return gridTelephonyPhoneDatatableDao.findAll(input);
	}

	@GetMapping("/rest/telephony/readOrgUnit")
	@ResponseBody
	public ResponseEntity<List<String>> readOrgUnit(@RequestHeader("phoneID") long phoneID) {
		TelephonyPhone phone = telephonyPhoneDao.findById(phoneID);
		if (phone == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		List<String> result = phone.getOrgUnits().stream().map(p -> p.getOrgunitUuid()).collect(Collectors.toList());
		
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
	
	@PostMapping("/rest/telephony/saveOrgUnit")
	@ResponseBody
	public ResponseEntity<HttpStatus> saveOrgUnit(@RequestHeader("phoneID") long phoneID, @RequestBody List<String> orgUnitUuids) {
		TelephonyPhone phone = telephonyPhoneDao.findById(phoneID);
		if (phone == null) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		boolean changes = false;

		// remove those no longer in the list
		for (Iterator<TelephonyPhoneOrgUnit> iterator = phone.getOrgUnits().iterator(); iterator.hasNext();) {
			TelephonyPhoneOrgUnit telephonyPhoneOrgUnit = iterator.next();
			
			if (!orgUnitUuids.contains(telephonyPhoneOrgUnit.getOrgunitUuid())) {
				iterator.remove();
				changes = true;
			}
		}

		// add new ones
		List<String> existingUuids = phone.getOrgUnits().stream().map(p -> p.getOrgunitUuid()).collect(Collectors.toList());
		for (String orgUnitUuid : orgUnitUuids) {
			if (!existingUuids.contains(orgUnitUuid)) {
				OrgUnit orgUnit = orgUnitService.getByUuid(orgUnitUuid);
				if (orgUnit != null) {
					TelephonyPhoneOrgUnit tp = new TelephonyPhoneOrgUnit();
					tp.setOrgunitUuid(orgUnit.getUuid());
					tp.setOrgunitName(orgUnit.getName());
					tp.setTelephonyPhone(phone);

					phone.getOrgUnits().add(tp);
					
					changes = true;
				}
			}
		}

		if (changes) {
			telephonyPhoneDao.save(phone);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping(value = "/rest/telephony/history")
	@ResponseBody
	public ResponseEntity<?> historySearch(@RequestBody String term) {
		List<TelephonyHistoryDTO> result = telephonyPhoneService.findHistoricPhoneHolders(term);
		
		return new ResponseEntity<>(result, HttpStatus.OK);
	}
}
