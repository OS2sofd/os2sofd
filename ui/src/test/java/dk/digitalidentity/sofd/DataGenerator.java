package dk.digitalidentity.sofd;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.dao.ClientDao;
import dk.digitalidentity.sofd.dao.OrgUnitDao;
import dk.digitalidentity.sofd.dao.PersonDao;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.Client;
import dk.digitalidentity.sofd.dao.model.Ean;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.Phone;
import dk.digitalidentity.sofd.dao.model.Post;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.enums.AccessRole;
import dk.digitalidentity.sofd.dao.model.enums.AffiliationType;
import dk.digitalidentity.sofd.dao.model.enums.PhoneType;
import dk.digitalidentity.sofd.dao.model.enums.VersionStatus;
import dk.digitalidentity.sofd.dao.model.mapping.AffiliationFunctionMapping;
import dk.digitalidentity.sofd.dao.model.mapping.OrgUnitPhoneMapping;
import dk.digitalidentity.sofd.dao.model.mapping.OrgUnitPostMapping;
import dk.digitalidentity.sofd.dao.model.mapping.PersonPhoneMapping;
import dk.digitalidentity.sofd.dao.model.mapping.PersonUserMapping;
import dk.digitalidentity.sofd.security.SecurityUtil;
import lombok.Getter;

@Component
@Getter
public class DataGenerator {

	@Autowired
	private DataGenerator generator;

	@Autowired
	private ClientDao clientDao;

	@Autowired
	private PersonDao personDao;

	@Autowired
	private OrgUnitDao orgUnitDao;

	private String apiKey = "TestApiKey";
	private String ouKommuneUuid = UUID.randomUUID().toString();
	private String ouHRUuid = UUID.randomUUID().toString();
	private String ouSundhedOgOmsorgUuid = UUID.randomUUID().toString();
	private String ouOmsorgUuid = UUID.randomUUID().toString();
	private String ouSundhedUuid = UUID.randomUUID().toString();
	private String user1Uuid = UUID.randomUUID().toString();
	private String user2Uuid = UUID.randomUUID().toString();
	private String user3Uuid = UUID.randomUUID().toString();
	private String user4Uuid = UUID.randomUUID().toString();
	private String user5Uuid = UUID.randomUUID().toString();
	private String user6Uuid = UUID.randomUUID().toString();
	private String user7Uuid = UUID.randomUUID().toString();
	private String user8Uuid = UUID.randomUUID().toString();
	private String user9Uuid = UUID.randomUUID().toString();
	private String user10Uuid = UUID.randomUUID().toString();

	// TODO: update with master/masterId where needed

	public void initData() {
		
		// before each test, we wipe all the data, and recreate, so we know the exact state between each test
		clientDao.deleteAll();
		personDao.deleteAll();
		orgUnitDao.deleteAll();

		// create client
		Client client = new Client();
		client.setApiKey(apiKey);
		client.setName("testclient");
		client.setAccessRole(AccessRole.WRITE_ACCESS);
		client.setVersionStatus(VersionStatus.UNKNOWN);
		clientDao.save(client);

		SecurityUtil.fakeLoginSession();

		// Create 5 orgUnits with this structure
		//
		//  kommune
		//    |
		//    |-- HR
		//    |
		//    |-- Sundhed og Omsorg
		//          |
		//          |-- Sundhed
		//          |
		//          |-- Omsorg
		//
		List<OrgUnit> units = new ArrayList<>();
		OrgUnit kommune = new OrgUnit();
		kommune.setCvr(12345678L);
		kommune.setEmail("email@email.dk");
		kommune.setEanList(new ArrayList<>());
		kommune.getEanList().add(new Ean(0, 576234545L, "TEST", true, kommune));
		
		Phone phone = Phone.builder().phoneNumber("12341234").master("TEST").masterId("TEST").prime(true).phoneType(PhoneType.LANDLINE).build();
		OrgUnitPhoneMapping phoneMapping = new OrgUnitPhoneMapping();
		phoneMapping.setOrgUnit(kommune);
		phoneMapping.setPhone(phone);
		kommune.getPhones().add(phoneMapping);
		
		phone = Phone.builder().phoneNumber("87878787").master("TEST").masterId("TEST").prime(false).phoneType(PhoneType.MOBILE).build();
		phoneMapping = new OrgUnitPhoneMapping();
		phoneMapping.setOrgUnit(kommune);
		phoneMapping.setPhone(phone);
		kommune.getPhones().add(phoneMapping);

		kommune.setSourceName("Kommune");
		kommune.setShortname("KOMMUNE");
		kommune.setUuid(ouKommuneUuid);
		kommune.setOrgType("OrgTypeString");
		kommune.setPnr(78787878L);
		kommune.setSenr(87654321L);
		kommune.setOrgTypeId(50L);
		kommune.setLocalExtensions("{\"key\":\"value\"}");
		kommune.setMaster("TEST");
		kommune.setMasterId(kommune.getUuid());
		units.add(kommune);

		OrgUnit hr = new OrgUnit();
		hr.setCvr(12345678L);
		hr.setEanList(new ArrayList<>());
		hr.getEanList().add(new Ean(0, 5758354545L, "TEST", true, hr));
		
		Post post = Post.builder().addressProtected(false).city("Viby J").country("DK").localname("localName").postalCode("8260").prime(true).street("Hasselager Centervej 17").master("TEST").masterId("TEST").build();
		OrgUnitPostMapping mapping = new OrgUnitPostMapping();
		mapping.setPost(post);
		mapping.setOrgUnit(hr);
		hr.getPostAddresses().add(mapping);
		
		hr.setSourceName("HR");
		hr.setShortname("HR");
		hr.setUuid(ouHRUuid);
		hr.setOrgType("OrgTypeString");
		hr.setParent(kommune);
		hr.setPnr(78787878L);
		hr.setSenr(87654321L);
		hr.setOrgTypeId(50L);
		hr.setMaster("TEST");
		hr.setMasterId(hr.getUuid());
		hr.setEmail("email_hr@email.dk");
		units.add(hr);

		OrgUnit sundhedOgOmsorg = new OrgUnit();
		sundhedOgOmsorg.setParent(kommune);
		sundhedOgOmsorg.setCvr(12345678L);
		sundhedOgOmsorg.setEanList(new ArrayList<>());
		sundhedOgOmsorg.getEanList().add(new Ean(0, 5743544545L, "TEST", true, sundhedOgOmsorg));
		
		post = Post.builder().addressProtected(false).city("Viby J").country("DK").localname("localName").postalCode("8260").prime(true).street("Hasselager Centervej 17").master("TEST").masterId("TEST").build();
		mapping = new OrgUnitPostMapping();
		mapping.setPost(post);
		mapping.setOrgUnit(sundhedOgOmsorg);

		sundhedOgOmsorg.getPostAddresses().add(mapping);		
		sundhedOgOmsorg.setSourceName("Sundhed og omsorg");
		sundhedOgOmsorg.setShortname("SUNDOMSORG");
		sundhedOgOmsorg.setUuid(ouSundhedOgOmsorgUuid);
		sundhedOgOmsorg.setOrgType("OrgTypeString");
		sundhedOgOmsorg.setPnr(78787878L);
		sundhedOgOmsorg.setSenr(87654321L);
		sundhedOgOmsorg.setOrgTypeId(50L);
		sundhedOgOmsorg.setMaster("TEST");
		sundhedOgOmsorg.setMasterId(sundhedOgOmsorg.getUuid());
		sundhedOgOmsorg.setEmail("email_soo@email.dk");

		units.add(sundhedOgOmsorg);

		OrgUnit sundhed = new OrgUnit();
		sundhed.setParent(sundhedOgOmsorg);
		sundhed.setCvr(12345678L);
		sundhed.setSourceName("Sundhed");
		sundhed.setShortname("SUND");
		sundhed.setUuid(ouSundhedUuid);
		sundhed.setOrgType("OrgTypeString");
		sundhed.setPnr(78787878L);
		sundhed.setSenr(87654321L);
		sundhed.setOrgTypeId(50L);
		sundhed.setMaster("TEST");
		sundhed.setMasterId(sundhed.getUuid());
		sundhed.setEmail("email_s@email.dk");
		sundhed.setEanList(new ArrayList<>());
		sundhed.getEanList().add(new Ean(0, 5746674534L, "TEST", true, sundhed));

		units.add(sundhed);

		OrgUnit omsorg = new OrgUnit();
		omsorg.setUuid(ouOmsorgUuid);
		omsorg.setParent(sundhedOgOmsorg);
		omsorg.setCvr(12345678L);
		omsorg.setSourceName("Omsorg");
		omsorg.setShortname("OMSORG");
		omsorg.setOrgType("OrgTypeString");
		omsorg.setPnr(78787878L);
		omsorg.setSenr(87654321L);
		omsorg.setOrgTypeId(50L);
		omsorg.setMaster("TEST");
		omsorg.setMasterId(omsorg.getUuid());
		omsorg.setEmail("email_om@email.dk");
		omsorg.setEanList(new ArrayList<>());
		omsorg.getEanList().add(new Ean(0, 5732112332L, "TEST", true, omsorg));

		units.add(omsorg);

		// save all created OUs
		orgUnitDao.save(units);

		// Create our 10 employees
		List<Person> persons = new ArrayList<>();

		for (int i = 0; i < 10 ; i ++) {
			String displayName = null;
			String cpr = null;
			String firstName = null;
			String surName = null;
			String uuid = null;
			String userId = null;
			OrgUnit positionOU1 = null;
			OrgUnit positionOU2 = null;

			switch (i) {
				case 0:
					displayName = "Hr. Hansen";
					cpr = "0112550980";
					firstName = "Hans";
					surName = "Hansen";
					uuid = user1Uuid;
					userId = "user1";
					positionOU1 = omsorg;
					break;
				case 1:
					cpr = "0404550990";
					firstName = "Grethe";
					surName = "Hansen";
					uuid = user2Uuid;
					userId = "user2";
					positionOU1 = omsorg;
					break;
				case 2:
					cpr = "0101201133";
					firstName = "Gert";
					surName = "Gunnerson";
					uuid = user3Uuid;
					userId = "user3";
					positionOU1 = sundhed;
					break;
				case 3:
					cpr = "0101551008";
					firstName = "Birthe";
					surName = "Biversen";
					uuid = user4Uuid;
					userId = "user4";
					positionOU1 = sundhed;
					break;
				case 4:
					cpr = "1234512345";
					firstName = "Hans";
					surName = "Jensen";
					uuid = user5Uuid;
					userId = "user5";
					positionOU1 = omsorg;
					break;
				case 5:
					cpr = "1234512346";
					firstName = "Jens";
					surName = "Hansen";
					uuid = user6Uuid;
					userId = "user6";
					positionOU1 = omsorg;
					break;
				case 6:
					cpr = "1234512347";
					firstName = "Morten";
					surName = "Sørensen";
					uuid = user7Uuid;
					userId = "user7";
					positionOU1 = omsorg;
					break;
				case 7:
					cpr = "1234512348";
					firstName = "Gurli";
					surName = "Gris";
					uuid = user8Uuid;
					userId = "user8";
					positionOU1 = omsorg;
					break;
				case 8:
					cpr = "1234512349";
					firstName = "Bente";
					surName = "Bomstærk";
					uuid = user9Uuid;
					userId = "user9";
					positionOU1 = hr;
					break;
				case 9:
					cpr = "1094778861";
					firstName = "Søren";
					surName = "Sørensen";
					uuid = user10Uuid;
					userId = "user10";
					positionOU1 = kommune;
					positionOU2 = hr;
					break;
			}

			Person person = new Person();
			person.setAnniversaryDate(new Date());
			person.setChosenName(displayName);
			person.setCpr(cpr);
			person.setMaster("TEST");
			person.setFirstEmploymentDate(new Date());
			person.setFirstname(firstName);
			person.setLocalExtensions("{\"key\":\"value\"}");
			
			phone = Phone.builder().phoneNumber("12341234").master("TEST").masterId("TEST").prime(true).phoneType(PhoneType.LANDLINE).build();
			PersonPhoneMapping personPhoneMapping = new PersonPhoneMapping();
			personPhoneMapping.setPhone(phone);
			personPhoneMapping.setPerson(person);
			person.getPhones().add(personPhoneMapping);

			phone = Phone.builder().phoneNumber("89898989").master("TEST").masterId("TEST").prime(false).phoneType(PhoneType.MOBILE).build();
			personPhoneMapping = new PersonPhoneMapping();
			personPhoneMapping.setPhone(phone);
			personPhoneMapping.setPerson(person);
			person.getPhones().add(personPhoneMapping);

			if (positionOU1 != null) {
				Affiliation position = new Affiliation();
				position.setEmployeeId("1");
				position.setEmploymentTerms("102");
				position.setEmploymentTermsText("102");
				position.setAffiliationType(AffiliationType.EMPLOYEE);
				position.setLocalExtensions("{\"key\":\"value\"}");
				
				position.setFunctions(new ArrayList<>());
				AffiliationFunctionMapping function = new AffiliationFunctionMapping();
				function.setAffiliation(position);
				function.setFunction("MED_UDVALG");
				position.getFunctions().add(function);

				position.setOrgUnit(positionOU1);
				position.setPayGrade("33");
				position.setPerson(person);
				position.setPositionId("41");
				position.setPositionName("Ansat");
				position.setStartDate(new Date());
				position.setWorkingHoursDenominator(37.0);
				position.setWorkingHoursNumerator(37.0);
				position.setUuid(UUID.randomUUID().toString());
				position.setMaster("TEST");
				position.setMasterId(position.getUuid());
				person.getAffiliations().add(position);
			}

			if (positionOU2 != null) {
				Affiliation position = new Affiliation();
				position.setEmployeeId("1");
				position.setEmploymentTerms("101");
				position.setEmploymentTermsText("101");
				position.setAffiliationType(AffiliationType.EMPLOYEE);
				position.setLocalExtensions("{\"key\":\"value\"}");

				position.setFunctions(new ArrayList<>());
				AffiliationFunctionMapping function = new AffiliationFunctionMapping();
				function.setAffiliation(position);
				function.setFunction("MED_UDVALG");
				position.getFunctions().add(function);				
				position.setOrgUnit(positionOU2);
				position.setPayGrade("33");
				position.setPerson(person);
				position.setPositionId("41");
				position.setPositionName("Ansat");
				position.setStartDate(new Date());
				position.setWorkingHoursDenominator(37.0);
				position.setWorkingHoursNumerator(37.0);
				position.setUuid(UUID.randomUUID().toString());
				position.setMaster("TEST");
				position.setMasterId(position.getUuid());
				person.getAffiliations().add(position);
			}

			person.setResidencePostAddress(Post.builder().addressProtected(true).city("Viby J").country("DK").localname("localName").postalCode("8260").prime(false).street("Hasselager Centervej 17").master("TEST").masterId("TEST").build());
			person.setRegisteredPostAddress(Post.builder().addressProtected(true).city("Viby J").country("DK").localname("localName").postalCode("8260").prime(true).street("Hasselager Centervej 17").master("TEST").masterId("TEST").build());

			person.setSurname(surName);
			User user = new User();
			user.setUserType("ACTIVE_DIRECTORY");
			user.setUuid(UUID.randomUUID().toString());
			user.setUserId(userId);
			user.setLocalExtensions("{\"key\":\"value\"}");
			user.setMaster("TEST");
			user.setMasterId(userId);
			
			PersonUserMapping uMapping = new PersonUserMapping();
			uMapping.setPerson(person);
			uMapping.setUser(user);

			person.getUsers().add(uMapping);
			
			// Bente get's an extra user
			if (i == 8) {
				user = new User();
				user.setUserType("UNILOGIN");
				user.setUuid("f78336af-6ef6-400b-bc6d-6a001787ab1f");
				user.setUserId("uniloginid");
				user.setMaster("TEST");
				user.setMasterId("uniloginid");
				
				uMapping = new PersonUserMapping();
				uMapping.setPerson(person);
				uMapping.setUser(user);
				
				person.getUsers().add(uMapping);				
			}
			
			person.setUuid(uuid);
			persons.add(person);
		}

		// save all users
		personDao.save(persons);
	}
}
