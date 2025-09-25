package dk.digitalidentity.sofd.util;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.PersistenceContext;

import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Organisation;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.Post;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.enums.AffiliationType;
import dk.digitalidentity.sofd.dao.model.mapping.AffiliationFunctionMapping;
import dk.digitalidentity.sofd.dao.model.mapping.PersonUserMapping;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.OrganisationService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import dk.digitalidentity.sofd.service.UserService;
import io.netty.util.internal.ThreadLocalRandom;
import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class BootstrapDevMode {
	private Random rand = new Random(Date.from(Instant.now()).getTime());
	private ArrayList<OrgUnit> orgUnits = new ArrayList<OrgUnit>();
	private Set<Integer> usedNumbers = new HashSet<>();
	
	@Value("${environment.dev:false}")
	private boolean devEnvironment;

	@PersistenceContext
	private EntityManager entityManager;

	@Autowired
	private OrgUnitService orgUnitService;

	@Autowired
	private OrganisationService organisationService;

	@Autowired
	private PersonService personService;

	@Autowired
	private UserService userService;

	@Autowired
	private BootstrapDevMode self;

	@EventListener(ApplicationReadyEvent.class)
	public void runOnStartup() throws Exception {
		self.init();
	}

	@Transactional(rollbackFor = Exception.class)
	public void init() throws Exception {
		// do not bootstrap if this is production
		if (!devEnvironment) {
			return;
		}

		entityManager.setFlushMode(FlushModeType.COMMIT);

		var stopWatch = new StopWatch();
		stopWatch.start();

		// do not bootstrap if there is already data in the system
		if (orgUnitService.getAll().size() == 0) {
			SecurityUtil.fakeLoginSession();

			createOrgUnits(rand.nextInt(100, 1400), 4, organisationService.getAdmOrg());

			generatePersons();
		}

		stopWatch.stop();

		log.info("Init finished: " + stopWatch.getTime() * 0.001 + " seconds");
	}

	@SuppressWarnings("deprecation")
	private void createOrgUnits(int maxOrgUnits, int maxDepth, Organisation org) throws Exception {
		var root = createOrgUnit(false, null, "rorg", "Root Organisation", org);
		createOrgUnitTree(1, root, maxOrgUnits, maxDepth + 1, org);

		orgUnitService.saveAll(orgUnits);
	}

	private void createOrgUnitTree(int curDepth, OrgUnit parent, int maxOrgUnits, int maxDepth, Organisation org) throws Exception {
		if (orgUnits.size() >= maxOrgUnits || curDepth >= maxDepth) {
			return;
		}

		var newNodeCount = rand.nextInt((int) Math.max(1, maxOrgUnits * 0.050) / (curDepth * 2)) + 1;

		if (parent.getChildren().size() == 0 && newNodeCount < 6) {
			newNodeCount = 6;
		}

		if (parent.getChildren().size() == 1 && newNodeCount == 1) {
			if (parent.getParent() != null) {
				return;
			}

			newNodeCount = rand.nextInt(2, (int) (maxOrgUnits * 0.075));
		}

		var children = new ArrayList<OrgUnit>();
		for (int i = 0; i < newNodeCount; i++) {
			if (orgUnits.size() >= maxOrgUnits) {
				return;
			}

			children.add(createOrgUnit(false, parent, "Org" + orgUnits.size(), "Organisation" + orgUnits.size(), org));
		}

		for (var child : children) {
			createOrgUnitTree(curDepth + 1, child, maxOrgUnits, maxDepth, org);
		}
	}

	private void generatePersons() {
		for (int i = 0; i < orgUnits.size() * 1.5; i++) {
			var person = createPerson("300200" + i);
			var affiliationCount = rand.nextInt(1, 3);
			var uniqueOrgUnits = getRandomUniqueOrgUnits(affiliationCount);
			
			for (int j = 0; j < affiliationCount; j++) {
				createAffiliation(person, uniqueOrgUnits.get(j));
			}
			
			// Generate AD users
			for (int j = 0; j < rand.nextInt(1, 5); j++) {
				createADUser(person);
			}

			personService.save(person);
		}
	}

	private ArrayList<OrgUnit> getRandomUniqueOrgUnits(int count) {
		var ret = new ArrayList<OrgUnit>();

		for (int i = 0; i < count; i++) {
			ret.add(orgUnits.get(0));
			Collections.shuffle(orgUnits);
		}

		return ret;
	}

	private OrgUnit createOrgUnit(boolean deleted, OrgUnit parentUnit, String shortName, String name, Organisation org) throws Exception {
		OrgUnit ret = new OrgUnit();
		ret.setUuid(UUID.randomUUID().toString());
		ret.setMaster("SOFD");
		ret.setMasterId(UUID.randomUUID().toString());

		ret.setDeleted(deleted);
		ret.setCreated(Date.from(Instant.now()));
		ret.setLastChanged(Date.from(Instant.now()));
		ret.setParent(parentUnit);
		ret.setShortname(shortName);
		ret.setSourceName(name);
		ret.setCvr(36074051l);
		ret.setOrgType("OrgTypeString");
		ret.setOrgTypeId(50l);

		ret.setBelongsTo(org);

		ret.setChildren(new ArrayList<>());

		orgUnits.add(ret);

		return ret;
	}

	private Person createPerson(String cpr) {
		var ret = new Person();
		ret.setUuid(UUID.randomUUID().toString());
		ret.setMaster("SOFD");
		ret.setCreated(Date.from(Instant.now()));
		ret.setLocalExtensions("{\"key\":\"value\"}");
		ret.setFirstname("Navn");
		ret.setSurname("Efter Navn");
		ret.setCpr(cpr);

		var startTime = LocalDate.of(1975, 1, 1).toEpochDay();
		var endTime = LocalDate.now().toEpochDay();
		var randomTime = ThreadLocalRandom.current().nextLong(startTime, endTime);

		ret.setFirstEmploymentDate(new Date(randomTime));

		ret.setResidencePostAddress(Post.builder().addressProtected(true).city("Viby J").country("DK")
				.localname("localName").postalCode("8260").prime(false).street("Hasselager Centervej 17").master("SOFD")
				.masterId(UUID.randomUUID().toString()).build());

		ret.setRegisteredPostAddress(Post.builder().addressProtected(true).city("Viby J").country("DK")
				.localname("localName").postalCode("8260").prime(true).street("Hasselager Centervej 17")
				.master("TESSOFDT").masterId(UUID.randomUUID().toString()).build());

		return ret;
	}

	private void createAffiliation(Person person, OrgUnit orgUnit) {
		var affiliation = new Affiliation();
		affiliation.setEmployeeId("" + rand.nextInt(50000));
		affiliation.setEmploymentTerms("101");
		affiliation.setEmploymentTermsText("Nogen terms");
		affiliation.setPayGrade("33");
		affiliation.setWorkingHoursDenominator(37.0);
		affiliation.setWorkingHoursNumerator(37.0);
		affiliation.setPerson(person);
		affiliation.setOrgUnit(orgUnit);
		affiliation.setAffiliationType(AffiliationType.EMPLOYEE);
		affiliation.setUuid(UUID.randomUUID().toString());
		affiliation.setStartDate(Date.from(Instant.now()));
		affiliation.setMaster("SOFD");
		affiliation.setMasterId(affiliation.getUuid());
		affiliation.setLocalExtensions("{\"key\":\"value\"}");
		affiliation.setPositionName("Sej ansat");
		affiliation.setPositionId("" + rand.nextInt(1, 9000));

		affiliation.setFunctions(new ArrayList<>());
		AffiliationFunctionMapping function = new AffiliationFunctionMapping();
		function.setAffiliation(affiliation);
		function.setFunction("MED_UDVALG");

		person.setAffiliations(new ArrayList<>());
		person.getAffiliations().add(affiliation);
	}

	private void createADUser(Person person) {
		if (person.getUsers() == null) {
			person.setUsers(new ArrayList<>());
		}

		var user = new User();
		user.setEmployeeId("" + rand.nextInt(50000));
		user.setUuid(UUID.randomUUID().toString());
		user.setMaster("SOFD");
		user.setMasterId(user.getUuid());
		user.setLocalExtensions("{\"key\":\"value\"}");
		user.setPrime(false);
		user.setUserId("user" + generateUniqueRandom(1,50000));
		user.setUserType(SupportedUserTypeService.getActiveDirectoryUserType());
		
		PersonUserMapping pum = new PersonUserMapping();
		pum.setPerson(person);
		pum.setUser(user);

		person.getUsers().add(pum);
		
		userService.save(user);
	}

	public int generateUniqueRandom(int lowerBound, int upperBound) {
		if (lowerBound >= upperBound) {
			throw new IllegalArgumentException("Lower bound must be less than upper bound.");
		}
		if (usedNumbers.size() >= (upperBound - lowerBound)) {
			throw new RuntimeException("All unique numbers have been generated.");
		}

		int candidate;
		do {
			candidate = rand.nextInt(upperBound - lowerBound) + lowerBound;
		} while (usedNumbers.contains(candidate));

		usedNumbers.add(candidate);
		return candidate;
	}
}
