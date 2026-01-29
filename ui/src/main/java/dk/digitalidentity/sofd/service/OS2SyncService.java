package dk.digitalidentity.sofd.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.ContactPlace;
import dk.digitalidentity.sofd.dao.model.FkOrgUuid;
import dk.digitalidentity.sofd.dao.model.Kle;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.Phone;
import dk.digitalidentity.sofd.dao.model.Post;
import dk.digitalidentity.sofd.dao.model.Setting;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.enums.CustomerSetting;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.PhoneType;
import dk.digitalidentity.sofd.dao.model.enums.TagType;
import dk.digitalidentity.sofd.dao.model.enums.Visibility;
import dk.digitalidentity.sofd.dao.model.mapping.ContactPlaceOrgUnitMapping;
import dk.digitalidentity.sofd.service.model.SyncResult;
import dk.digitalidentity.sofd.service.os2sync.dto.FKOU;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OS2SyncService {
	private static final String updateUserSQL = "INSERT INTO queue_users (uuid, user_id, phone_number, email, racfid, name, cpr, landline, cvr, priority, operation) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'UPDATE');";
	private static final String deleteUserSQL = "INSERT INTO queue_users (uuid, cvr, operation) VALUES (?, ?, 'DELETE');";
	private static final String updatePositionSQL = "INSERT INTO queue_user_positions (user_id, name, orgunit_uuid, start_date, stop_date) VALUES (?, ?, ?, ?, ?);";
	private static final String updateOrgUnitSQL = "INSERT INTO queue_orgunits (uuid, name, manager_uuid, parent_ou_uuid, los_shortname, phone_number, email, ean, post_address, orgunit_type, contact_open_hours, contact, landline, cvr, operation, location, url, email_remarks, post_return, phone_open_hours, losid, pnr) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'UPDATE', ?, ?, ?, ?, ?, ?, ?);";
	private static final String deleteOrgUnitSQL = "INSERT INTO queue_orgunits (uuid, cvr, operation) VALUES (?, ?, 'DELETE');";
	private static final String updateOrgUnitTaskSQL = "INSERT INTO queue_orgunits_tasks (unit_id, task) VALUES (?, ?);";
	private static final String updateOrgUnitTagSQL = "INSERT INTO queue_orgunits_it_systems (unit_id, it_system_uuid) VALUES (?, ?);";
	private static final String updateOrgUnitContactPlaceSQL = "INSERT INTO queue_orgunits_contact_for_tasks (unit_id, task) VALUES (?, ?);";
	private static final String updateOrgUnitContactPlaceUsersSQL = "INSERT INTO queue_orgunits_contact_places (unit_id, contact_place_uuid) VALUES (?, ?);";

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private FkOrgUuidService fkOrgUuidService;

	@Autowired
	private KleService kleService;

	@Autowired
	private OrgUnitService orgUnitService;

	@Autowired
	private PersonService personService;

	@Autowired
	private ContactPlaceService contactPlaceService;

	@Autowired
	private SyncService syncService;

	@Autowired
	private SettingService settingService;

	@Autowired
	private FkOrganisationService fkOrganisationService;

	@Autowired
	private OrganisationService organisationService;
	
	@Autowired
	private UserService userService;

	@Qualifier("OS2syncTemplate")
	@Autowired(required = false)
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EanService eanService;
	
	@Qualifier("defaultRestTemplate")
	@Autowired
	private RestTemplate restTemplate;

	@Transactional
	public void synchronizeHierarchy() {
		if (!configuration.getScheduled().isEnabled() || !configuration.getIntegrations().getOs2sync().isEnabled()) {
			log.info("OS2sync synchronization disabled on this instance");
			return;
		}

		log.info("Peforming cleanup of orgunits in FK Organisation");

		var admOrg = organisationService.getAdmOrg();

		// read all OrgUnits from FK Organisation
		List<FKOU> fkOrgUnits;
		try {
			fkOrgUnits = fkOrganisationService.getOrgUnits();
		}
		catch (Exception ex) {
			log.warn("Unable to fetch OrgUnits from FK Organization.", ex);
			return;
		}

		// read all OrgUnits from SOFD Core (active only)
		List<OrgUnit> sofdOrgUnits = orgUnitService.getAll().stream().filter(o -> !o.isDeleted() && !o.isDoNotTransferToFkOrg()).toList();

		// read contactPlaces
		List<ContactPlace> allContactPlaces = (configuration.getModules().getContactPlaces().isEnabled()) ? contactPlaceService.findAll() : null;

		// find all OrgUnits (in SOFD) that are not currently in FK Organisation and trigger a sync on those through OS2sync
		for (OrgUnit orgUnit : sofdOrgUnits) {
			if (fkOrgUnits.stream().noneMatch(fkou -> Objects.equals(fkou.getUuid(), orgUnit.getUuid()))) {
				log.info("Weekly sync found OU not present in FK Organisation: " + orgUnit.getName() + " / " + orgUnit.getUuid());
				updateOrgUnit(orgUnit, allContactPlaces, admOrg.getId());
			}
		}

		// find all OrgUnits in FK Organisation that are no longer in SOFD (or are deleted = true in SOFD), and trigger a delete on those through OS2sync
		for (FKOU fkou : fkOrgUnits) {
			Optional<OrgUnit> foundOrgUnit = sofdOrgUnits.stream()
					.filter(sofdOrgUnit -> Objects.equals(sofdOrgUnit.getUuid(), fkou.getUuid()))
					.findAny();

			if (!foundOrgUnit.isPresent() || foundOrgUnit.get().isDeleted()) {
				log.info("Weekly sync found OU that needs to be removed from FK Organisation: " + fkou.getName() + " / " + fkou.getUuid());
				deleteOrgUnit(fkou.getUuid());
			}
		}

		log.info("Cleanup completed");
	}
	
	@Transactional
	public void cleanupUsers() {
		if (!configuration.getScheduled().isEnabled() || !configuration.getIntegrations().getOs2sync().isEnabled()) {
			log.info("OS2sync synchronization disabled on this instance - not running cleanup");
			return;
		}

		log.info("Peforming cleanup of users in FK Organisation");
		
		HttpHeaders headers = new HttpHeaders();
		headers.add("Cvr", configuration.getCustomer().getCvr());
		headers.add("Accept", "application/json");
		headers.setContentType(MediaType.APPLICATION_JSON);

		var kombitUuids = userService.getAllKombitUuids();
		// failsafe
		var failsafeThreshold = 1000;
		if( kombitUuids.size() < failsafeThreshold ) {
			log.info("Amount of kombitUuids was less than expected (" + failsafeThreshold + ") - not running cleanup");
			return;
		}

		var request = new HttpEntity<>(kombitUuids, headers);
		ResponseEntity<String> response = restTemplate.exchange(configuration.getIntegrations().getOs2sync().getUserCleanupUrl(), HttpMethod.POST, request, String.class);
		
		if (response.getStatusCodeValue() != 200) {
			log.warn("Cleanup of FK organisation users failed: " + response.getStatusCodeValue());
		}
		
		log.info("Users cleanup completed");
	}

	@Transactional
	public void fullUpdate() {
		Long head = syncService.getMaxOffset();
		if (head == null) {
			head = 0L;
		}

		var admOrg = organisationService.getAdmOrg();

		List<ContactPlace> allContactPlaces = (configuration.getModules().getContactPlaces().isEnabled()) ? contactPlaceService.findAll() : null;

		List<OrgUnit> orgUnits = orgUnitService.getAll();
		Set<String> doNotTransferToFKOrgUuids = orgUnitService.getDoNotTransferToFKOrgUuids();

		for (OrgUnit orgUnit : orgUnits) {
			if (orgUnit.isDeleted() || doNotTransferToFKOrgUuids.contains(orgUnit.getUuid()) ) {
				deleteOrgUnit(orgUnit.getUuid());
			}
			else {
				updateOrgUnit(orgUnit, allContactPlaces, admOrg.getId());
			}
		}

		List<Person> persons = personService.getAll();
		for (Person person : persons) {
			if (person.isDeleted()) {
				deletePerson(person);
			}
			else {
				updatePerson(person, doNotTransferToFKOrgUuids, admOrg.getId());
			}
		}

		updateLatestSTSSyncRun(head);
	}

	@Transactional
	public void deltaUpdate(Long lastRun) {
		Long updatedLastRun = lastRun;

		SyncResult syncResult = syncService.getModificationHistory(lastRun, EntityType.ORGUNIT.toString());
		Set<String> uuids = syncResult.getUuids().stream().map(w -> w.getUuid()).collect(Collectors.toSet());
		Set<String> doNotTransferToFKOrgUuids = orgUnitService.getDoNotTransferToFKOrgUuids();
		var admOrg = organisationService.getAdmOrg();


		List<ContactPlace> allContactPlaces = null;
		if (uuids.size() > 0 && configuration.getModules().getContactPlaces().isEnabled()) {
			allContactPlaces = contactPlaceService.findAll();
		}

		for (String uuid : uuids) {
			OrgUnit orgUnit = orgUnitService.getByUuid(uuid);

			if (orgUnit == null || orgUnit.isDeleted() || doNotTransferToFKOrgUuids.contains(uuid) ) {
				deleteOrgUnit(uuid);
			}
			else {
				updateOrgUnit(orgUnit, allContactPlaces, admOrg.getId());
			}
		}

		if (syncResult.getOffset() > updatedLastRun) {
			updatedLastRun = syncResult.getOffset();
		}

		syncResult = syncService.getModificationHistory(lastRun, EntityType.PERSON.toString());
		uuids = syncResult.getUuids().stream().map(w -> w.getUuid()).collect(Collectors.toSet());

		for (String uuid : uuids) {
			Person person = personService.getByUuid(uuid);
			if (person == null) {
				log.debug("Skipping " + uuid + " because no person exists");
				continue;
			}

			if (person.isDeleted()) {
				deletePerson(person);
			}
			else {
				updatePerson(person, doNotTransferToFKOrgUuids, admOrg.getId());
			}
		}

		if (syncResult.getOffset() > updatedLastRun) {
			updatedLastRun = syncResult.getOffset();
		}

		if (updatedLastRun > lastRun) {
			updateLatestSTSSyncRun(updatedLastRun);
		}

		// sync contactPlaces if any have changed since last run
		if (configuration.getModules().getContactPlaces().isEnabled()) {
			List<ContactPlace> contactPlaces = contactPlaceService.findModified();
			if (contactPlaces.size() > 0) {
				Map<String, OrgUnit> mapContactPlaces = new HashMap<>();
				Map<String, OrgUnit> mapUsers = new HashMap<>();

				for (ContactPlace contactPlace : contactPlaces) {
					mapContactPlaces.put(contactPlace.getContactPlace().getUuid(), contactPlace.getContactPlace());

					for (ContactPlaceOrgUnitMapping mapping : contactPlace.getUsers()) {
						mapUsers.put(mapping.getOrgUnit().getUuid(), mapping.getOrgUnit());
					}
				}

				if (allContactPlaces == null) {
					allContactPlaces = contactPlaceService.findAll();
				}

				// now send update to OS2sync (contactPlaces first, so we are sure they are created before we send "users")
				for (OrgUnit orgUnit : mapContactPlaces.values()) {
					if (!orgUnit.isDeleted()) {
						updateOrgUnit(orgUnit, allContactPlaces, admOrg.getId());
					}
				}

				for (OrgUnit orgUnit : mapUsers.values()) {
					if (!orgUnit.isDeleted()) {
						updateOrgUnit(orgUnit, allContactPlaces, admOrg.getId());
					}
				}

				// cleanup in database
				for (ContactPlace contactPlace : contactPlaces) {
					if (contactPlace.isDeleted()) {
						contactPlaceService.delete(contactPlace);
					}
					else {
						contactPlace.setSynchronizedToOrganisation(true);

						for (Iterator<ContactPlaceOrgUnitMapping> iterator = contactPlace.getUsers().iterator(); iterator.hasNext();) {
							ContactPlaceOrgUnitMapping mapping = iterator.next();

							if (mapping.isDeleted()) {
								iterator.remove();
							}
						}

						contactPlaceService.save(contactPlace);
					}
				}
			}
		}
	}

	public void deletePerson(Person person) {
		log.debug("Deleting " + person.getUuid());
		
		List<FkOrgUuid> fkOrgUuidEntries = fkOrgUuidService.getByPersonUuid(person.getUuid());

		// delete any user registered in fk org uuid table
		for (FkOrgUuid entry : fkOrgUuidEntries) {
			deleteUser(entry.getKombitUuid());
			fkOrgUuidService.delete(entry);
		}

		// also delete any user that is currently registered on the person
		for( var user : person.getUsers() ) {
			if( SupportedUserTypeService.isActiveDirectory(user.getUser().getUserType()) ) {
				var adDetails = user.getUser().getActiveDirectoryDetails();
				if( adDetails != null && fkOrgUuidEntries.stream().noneMatch(e -> e.getKombitUuid().equalsIgnoreCase(adDetails.getKombitUuid())) ) {
					deleteUser(adDetails.getKombitUuid());
				}
			}
		}

	}

	public void updatePerson(Person person, Set<String> doNotTransferToFKOrgUuids, long admOrgId) {
		log.debug("Updating " + person.getUuid());
		
		List<Affiliation> activeAffiliations = AffiliationService.notStoppedAffiliations(person.getAffiliations()).stream()
			.filter(a -> a.getOrgUnit().getBelongsTo().getId() == admOrgId && a.isDoNotTransferToFkOrg() == false && !doNotTransferToFKOrgUuids.contains(a.getOrgUnit().getUuid()))
			.collect(Collectors.toList());

		// no active affiliations, just map it to a delete event instead
		if (activeAffiliations.size() == 0) {
			deletePerson(person);
			return;
		}

		// find mobile phone number
		var phoneValue = PersonService.getPhones(person).stream().filter(p ->
				p.getPhoneType().equals(PhoneType.MOBILE)
				&& (p.getVisibility() == Visibility.VISIBLE || configuration.getIntegrations().getOs2sync().isSendHiddenPhoneNumbers())) // only send hidden phonenumbers if configured to do so
				.sorted(Comparator.comparing(Phone::isTypePrime).reversed().thenComparing(Phone::getPhoneNumber)).findFirst() // sort by typeprime (if present), then by phone number to prevent flip-flopping between multiple valid numbers.
				.map(Phone::getPhoneNumber).orElse(null);

		// find landline phone number
		Optional<Phone> landline = PersonService.getPhones(person).stream().filter(p -> p.isTypePrime() && p.getPhoneType().equals(PhoneType.LANDLINE)).findFirst();
		String landlineValueTemp = null;

		if (!landline.isPresent()) {
			Optional<Phone> ip = PersonService.getPhones(person).stream().filter(p -> p.isTypePrime() && p.getPhoneType().equals(PhoneType.IP)).findFirst();

			if (!ip.isPresent()) {
				Optional<Phone> broadBand = PersonService.getPhones(person).stream().filter(p -> p.isTypePrime() && p.getPhoneType().equals(PhoneType.BROADBAND)).findFirst();

				if (!broadBand.isPresent()) {
					landlineValueTemp = null;
				}
				else {
					landlineValueTemp = broadBand.get().getPhoneNumber();
				}
			}
			else {
				landlineValueTemp = ip.get().getPhoneNumber();
			}
		}
		else {
			landlineValueTemp = landline.get().getPhoneNumber();
		}
		final String landlineValue = landlineValueTemp;

		// find prime kspCics
		Optional<User> kspCicsUser = PersonService.getUsers(person).stream().filter(u -> SupportedUserTypeService.isKspCics(u.getUserType()) && u.isPrime()).findFirst();
		final String kspCicsValue = (kspCicsUser.isPresent()) ? kspCicsUser.get().getUserId() : null;

		// find name
		final String nameValue = (person.getChosenName() != null) ? person.getChosenName() : (person.getFirstname() + " " + person.getSurname());

		List<User> adUsers = PersonService.getUsers(person)
				.stream()
				.filter(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType())
					|| (configuration.getIntegrations().getOs2sync().isSchoolEnabled() &&
							// OBS! We currently assume that AzureAD is always from a school domain (that will bite us later ;))
							(SupportedUserTypeService.isActiveDirectorySchool(u.getUserType()) || SupportedUserTypeService.isAzureAd(u.getUserType()))))
				.collect(Collectors.toList());

		// filter out affiliations that are linked to a single user account
		List<Affiliation> unlinkedAffiliations = new ArrayList<>(activeAffiliations);
		for (User adUser : adUsers) {
			if (StringUtils.hasLength(adUser.getEmployeeId())) {
				unlinkedAffiliations.removeIf(a -> Objects.equals(a.getEmployeeId(), adUser.getEmployeeId()));
			}
		}

		// list of users that we have already synchronized to FK Organisation (for later cleanup and tracking)
		List<FkOrgUuid> entries = fkOrgUuidService.getByPersonUuid(person.getUuid());

		for (User adUser : adUsers) {
			log.debug("Looking at " + adUser.getUserId() + " for update");

			// what set of affiliations are we going to use for this specific AD account?
			List<Affiliation> filteredAffiliations;
			if (StringUtils.hasLength(adUser.getEmployeeId())) {
				// use directly mapped affiliation
				filteredAffiliations = activeAffiliations.stream()
						.filter(a -> Objects.equals(a.getEmployeeId(), adUser.getEmployeeId()))
						.collect(Collectors.toList());
			}
			else if(SupportedUserTypeService.isActiveDirectorySchool(adUser.getUserType())) {
				// it's a school AD account - use any active affiliation
				filteredAffiliations = activeAffiliations;
			}
			else {
				// use affiliations not linked to another user
				filteredAffiliations = unlinkedAffiliations;
			}

			if (filteredAffiliations.size() == 0) {
				log.warn("Skipping synchronization on " + adUser.getUserId() + " because no affiliations are available for this account");
				continue;
			}

			String emailValue = null;
			if (SupportedUserTypeService.isActiveDirectory(adUser.getUserType())) {
				Optional<User> emailUser = PersonService.getUsers(person).stream()
						.filter(u -> SupportedUserTypeService.isExchange(u.getUserType()) && Objects.equals(u.getMasterId(), adUser.getUserId()))
						.findFirst();

				emailValue = (emailUser.isPresent()) ? emailUser.get().getUserId() : null;
			}
			else if (SupportedUserTypeService.isActiveDirectorySchool(adUser.getUserType())) {
				Optional<User> emailUser = PersonService.getUsers(person).stream()
						.filter(u -> SupportedUserTypeService.isSchoolEmail(u.getUserType()) && Objects.equals(u.getMasterId(), adUser.getUserId()))
						.findFirst();

				emailValue = (emailUser.isPresent()) ? emailUser.get().getUserId() : null;
			}
			else if (SupportedUserTypeService.isAzureAd(adUser.getUserType())) {
				// in Azure, the userId is actually an email address
				emailValue = adUser.getUserId();
			}

			boolean blankCpr = false;
			if (!configuration.getIntegrations().getOs2sync().isCprEnabled()) {
				blankCpr = true;
			}
			else if (configuration.getIntegrations().getOs2sync().isDoNotSendCprForSubstitutes()) {
				if (UserService.isSubstituteUser(adUser)) {
					blankCpr = true;
				}
			}
			// temporary - because Nexus does not support multiple accounts with same cpr
			else if (adUser.isPrime() == false) {
				blankCpr = true;
			}

			final String cprValue = (!blankCpr) ? person.getCpr() : null;

			// because the "adUser" might be a STIL user, we need to allow the person.uuid to be used here (though that introduces a bunch of issues
			// at a later point, should that school-employee switch to being an administrative employee *sigh*
			final String finalUuid = (adUser.getActiveDirectoryDetails() != null && StringUtils.hasLength(adUser.getActiveDirectoryDetails().getKombitUuid()))
					? adUser.getActiveDirectoryDetails().getKombitUuid()
					: (SupportedUserTypeService.isAzureAd(adUser.getUserType()) ? adUser.getMasterId() : person.getUuid());
			
			final String finalEmailValue = emailValue;
			final String finalUserId = (SupportedUserTypeService.isAzureAd(adUser.getUserType()) ? getEmailSuffix(adUser.getUserId()) : adUser.getUserId());

			// we want to fasttrack sync of substitute users (vikXXXX)
			final long priority = (UserService.isSubstituteUser(adUser)) ? 8 : 10;

			if (adUser.isDisabled()) {
				deleteUser(finalUuid);

				FkOrgUuid entry = entries.stream().filter(e -> Objects.equals(finalUuid, e.getKombitUuid())).findFirst().orElse(null);
				if (entry != null) {
					entries.remove(entry);
					fkOrgUuidService.delete(entry);
				}
			}
			else {
				log.debug("Transfering " + adUser.getUserId() + " to OS2sync");
				
				GeneratedKeyHolder holder = new GeneratedKeyHolder();
				jdbcTemplate.update(new PreparedStatementCreator() {

					@Override
					public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
						PreparedStatement statement = con.prepareStatement(updateUserSQL, Statement.RETURN_GENERATED_KEYS);
						statement.setString(1, finalUuid);
						statement.setString(2, finalUserId);
						statement.setString(3, StringUtils.hasText(phoneValue) ? phoneValue : null);
						statement.setString(4, finalEmailValue);
						statement.setString(5, kspCicsValue);
						statement.setString(6, nameValue);
						statement.setString(7, cprValue);
						statement.setString(8, StringUtils.hasText(landlineValue) ? landlineValue : null);
						statement.setString(9, configuration.getCustomer().getCvr());
						statement.setLong(10, priority);

						return statement;
					}
				}, holder);

				final Long primaryKey = holder.getKey().longValue();

				filteredAffiliations.forEach(a -> jdbcTemplate.update(updatePositionSQL, new Object[] {
					primaryKey,
					AffiliationService.getPositionName(a),
					a.getCalculatedOrgUnit().getUuid(),
					formatDate(a.getStartDate(), false),
					formatDate(a.getStopDate(), true)
				}));

				FkOrgUuid entry = entries.stream().filter(e -> Objects.equals(finalUuid, e.getKombitUuid())).findFirst().orElse(null);
				if (entry == null) {
					entry = new FkOrgUuid();
					entry.setKombitUuid(finalUuid);
					entry.setPersonUuid(person.getUuid());
					entry.setUserId(finalUserId);

					entry = fkOrgUuidService.save(entry);
					entries.add(entry);
				}
			}
		}

		for (FkOrgUuid entry : entries) {
			boolean found = false;

			for (User adUser : adUsers) {
				// match on userId to avoid issue with ActiveDirectoryDetails being null on STIL users
				if (Objects.equals(adUser.getUserId(), entry.getUserId())) {
					found = true;
					break;
				}
			}

			if (!found) {
				deleteUser(entry.getKombitUuid());
				fkOrgUuidService.delete(entry);
			}
		}
	}

	private String getEmailSuffix(String userId) {
		if (!StringUtils.hasLength(userId)) {
			return userId;
		}
		
		int idx = userId.indexOf("@");
		if (idx < 0) {
			return userId;
		}

		return userId.substring(0, idx);
	}

	private void updateOrgUnit(OrgUnit orgUnit, List<ContactPlace> contactPlaces, long admOrgId) {
		// ignore if orgunit does not belong ot administrative organisation
		if( orgUnit.getBelongsTo().getId() != admOrgId ) {
			return;
		}

		// find name
		final String nameValue = orgUnit.getName();

		// find parent
		final String parentUuid = (orgUnit.getParent() != null) ? orgUnit.getParent().getUuid() : null;

		String phoneValueTemp = null;
		String landlineValueTemp = null;
		// special handling of phone transfers to FK Org for orgunits tagged with SOR
		if( orgUnit.getTags().stream().anyMatch(t -> t.getTag().getTagType() == TagType.SOR)) {
			// No value is sent to FK Org "Telefonnummer" felt => the FK Org->SOR sync will use "Fastnetnummer" as fallback
			phoneValueTemp = null; // yeah, redundant line...here for readability
			// The primary phone number regardless of type is sent to FK Org "Fastnetnummer" and from there to SOR Telefonnummer
			landlineValueTemp = OrgUnitService.getPhones(orgUnit).stream().filter(Phone::isPrime).findFirst().map(Phone::getPhoneNumber).orElse(null);
		}
		// normal handling of phone transfers (orgunits not tagged with SOR)
		else
		{
			// find mobile phone number
			Optional<Phone> phone = OrgUnitService.getPhones(orgUnit).stream().filter(p -> p.isTypePrime() && p.getPhoneType().equals(PhoneType.MOBILE)).findFirst();
			if (!phone.isPresent()) {
				Optional<Phone> prime = OrgUnitService.getPhones(orgUnit).stream().filter(p -> p.isPrime()).findFirst();

				if (!prime.isPresent()) {
					phoneValueTemp = null;
				}
				else {
					phoneValueTemp = prime.get().getPhoneNumber();
				}
			}
			else {
				phoneValueTemp = phone.get().getPhoneNumber();
			}

			// find landline phone number
			Optional<Phone> landline = OrgUnitService.getPhones(orgUnit).stream().filter(p -> p.isTypePrime() && p.getPhoneType().equals(PhoneType.LANDLINE)).findFirst();
			if (!landline.isPresent()) {
				Optional<Phone> ip = OrgUnitService.getPhones(orgUnit).stream().filter(p -> p.isTypePrime() && p.getPhoneType().equals(PhoneType.IP)).findFirst();

				if (!ip.isPresent()) {
					Optional<Phone> broadBand = OrgUnitService.getPhones(orgUnit).stream().filter(p -> p.isTypePrime() && p.getPhoneType().equals(PhoneType.BROADBAND)).findFirst();

					if (!broadBand.isPresent()) {
						landlineValueTemp = null;
					}
					else {
						landlineValueTemp = broadBand.get().getPhoneNumber();
					}
				}
				else {
					landlineValueTemp = ip.get().getPhoneNumber();
				}
			}
			else {
				landlineValueTemp = landline.get().getPhoneNumber();
			}
		}
		final String phoneValue = phoneValueTemp;
		final String landlineValue = landlineValueTemp;


		// find EAN
		String eanValue = eanService.getEan(orgUnit);

		// find KLE
		List<String> tasks = new ArrayList<>();
		for (String code : orgUnitService.getKleCodesIncludingInherited(orgUnit)) {
			Kle kle = kleService.getByCode(code);

			// TODO: KMD has started blocking updates if any KLE is inactive, and we have raised a change request
			//       on that, but for now we need to filter the inactive KLE out during updates
			if (kle != null && StringUtils.hasLength(kle.getUuid()) && kle.isActive()) {
//			if (kle != null && StringUtils.hasLength(kle.getUuid())) {
				tasks.add(kle.getUuid());
			}
		}

		// find prime post address
		Optional<Post> post = OrgUnitService.getPosts(orgUnit).stream().filter(p -> p.isPrime()).findFirst();
		String tmpPostValue = null;
		if (post.isPresent()) {
			Post p = post.get();
			tmpPostValue = p.getAsOneLine();
		}
		final String postValue = tmpPostValue;

		final String orgUnitType;
		if ("TEAM".equals(orgUnit.getType().getKey())) {
			orgUnitType = "TEAM";
		}
		else {
			orgUnitType = "DEPARTMENT";
		}

		// open hours for this orgUnit
		final String contactOpenHours;
		if (StringUtils.hasText(orgUnit.getOpeningHours())) {
			if (orgUnit.getOpeningHours().length() > 200) {
				contactOpenHours = orgUnit.getOpeningHours().substring(0, 200);
			}
			else {
				contactOpenHours = orgUnit.getOpeningHours();
			}
		}
		else {
			contactOpenHours = null;
		}

		// find location
		final String location = StringUtils.hasText(orgUnit.getLocation()) ? orgUnit.getLocation() : null;
		// find url
		final String url = StringUtils.hasText(orgUnit.getUrlAddress()) ? orgUnit.getUrlAddress() : null;
		// find email notes
		final String emailNotes = StringUtils.hasText(orgUnit.getEmailNotes()) ? orgUnit.getEmailNotes() : null; 

		// find return address
		Optional<Post> returnPost = OrgUnitService.getPosts(orgUnit).stream().filter(p -> p.isReturnAddress()).findFirst();
		String tempReturnPost = null;
		if (returnPost.isPresent()) {
			Post p = returnPost.get();
			tempReturnPost = p.getAsOneLine();
		}
		final String returnAddress = tempReturnPost;

		// find phone opening hours
		final String openingHoursPhone = StringUtils.hasText(orgUnit.getOpeningHoursPhone()) ? orgUnit.getOpeningHoursPhone() : null;

		// find "Henvendelsessted"
		final String contact = StringUtils.hasText(orgUnit.getContactAddress()) ? orgUnit.getContactAddress() : null;
		
		// for OPUS owned units, supply the losid and shortname as well
		String losId = null;
		String losValue = null;
		if (Objects.equals(orgUnit.getMaster(), "OPUS")) {
			losId = orgUnit.getMasterId();
			losValue = orgUnit.getShortname();
		}
		else {
			// check if the OrgUnits are tagged with losId or losValue and use those tag values (ie. Odsherred)
			var losIdTag = orgUnit.getTags().stream().filter(t -> t.getTag().getTagType() == TagType.LOSID).findFirst().orElse(null);
			losId = losIdTag == null ? null : losIdTag.getCustomValue();

			var losValueTag = orgUnit.getTags().stream().filter(t -> t.getTag().getTagType() == TagType.LOSVALUE).findFirst().orElse(null);
			losValue = losValueTag == null ? null : losValueTag.getCustomValue();
		}

		final String finalLosId = losId;
		final String finalLosValue = losValue;

		final String pnr = (orgUnit.getPnr() != null && orgUnit.getPnr() != 0) ? (Long.toString(orgUnit.getPnr())) : null;

		// find manager
		String tManagerUuid = null;
		if (orgUnit.getManager() != null && orgUnit.getManager().getManager() != null) {
			List<User> users = PersonService.getUsers(orgUnit.getManager().getManager()).stream().filter(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType())).toList();

			User managerUser = null;

			// check if a user is mapped to an affiliation in the current OrgUnit
			for (var user : users) {
				if (StringUtils.hasText(user.getEmployeeId())) {
					for (var affiliation : orgUnit.getManager().getManager().getAffiliations()) {
						if (affiliation.getEmployeeId().equalsIgnoreCase(user.getEmployeeId()) && affiliation.getOrgUnit().getUuid().equalsIgnoreCase(orgUnit.getUuid())) {
							managerUser = user;
							break;
						}
					}
					
					if (managerUser != null) {
						break;
					}
				}
			}

			if (managerUser == null) {
				// fallback to using the prime user
				managerUser = users.stream().filter(u -> u.isPrime()).findFirst().orElse(null);
			}

			if (managerUser != null && managerUser.getActiveDirectoryDetails() != null) {
				tManagerUuid = managerUser.getActiveDirectoryDetails().getKombitUuid();
			}
		}
		final String managerUuid = tManagerUuid;

		final String emailValue = StringUtils.hasText(orgUnit.getEmail()) ? orgUnit.getEmail() : null;
		
		GeneratedKeyHolder holder = new GeneratedKeyHolder();
		jdbcTemplate.update(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement statement = con.prepareStatement(updateOrgUnitSQL, Statement.RETURN_GENERATED_KEYS);
				statement.setString(1, orgUnit.getUuid());
				statement.setString(2, nameValue);
				statement.setString(3, managerUuid);
				statement.setString(4, parentUuid);
				statement.setString(5, finalLosValue);
				statement.setString(6, phoneValue);
				statement.setString(7, emailValue);
				statement.setString(8, eanValue);
				statement.setString(9, postValue);
				statement.setString(10, orgUnitType);
				statement.setString(11, contactOpenHours);
				statement.setString(12, contact);
				statement.setString(13, landlineValue);
				statement.setString(14, configuration.getCustomer().getCvr());
				statement.setString(15, location);
				statement.setString(16, url);
				statement.setString(17, emailNotes);
				statement.setString(18, returnAddress);
				statement.setString(19, openingHoursPhone);
				statement.setString(20, finalLosId);
				statement.setString(21, pnr);

				return statement;
			}
		}, holder);

		final Long primaryKey = holder.getKey().longValue();

		// insert Tasks
		if (tasks.size() > 0) {
			tasks.stream().forEach(t -> jdbcTemplate.update(updateOrgUnitTaskSQL, new Object[] { primaryKey, t }));
		}

		// insert Tags
		if (!orgUnit.getTags().isEmpty()) {
			orgUnit.getTags().stream()
				.filter(tag -> tag.getTag().getTagType() == TagType.IT_SYSTEM)
				.forEach(tag -> jdbcTemplate.update(updateOrgUnitTagSQL, new Object[] { primaryKey, tag.getTag().getItSystemUuid() }));
		}

		if (configuration.getModules().getContactPlaces().isEnabled() && contactPlaces != null) {
			for (ContactPlace contactPlace : contactPlaces) {
				// we skip deleted or empty (taskwise) contactPlaces - those are present only to force a sync of the OrgUnits
				if (contactPlace.isDeleted() || contactPlace.getTasks().size() == 0) {
					continue;
				}

				if (contactPlace.getContactPlace().getUuid().equals(orgUnit.getUuid())) {
					contactPlace.getTasks().stream()
						.forEach(task -> jdbcTemplate.update(updateOrgUnitContactPlaceSQL, new Object[] { primaryKey, task.getKle().getUuid() }));
				}
				else if (contactPlace.getUsers().stream().anyMatch(m -> !m.isDeleted() && m.getOrgUnit().getUuid().equals(orgUnit.getUuid()))) {
					jdbcTemplate.update(updateOrgUnitContactPlaceUsersSQL, new Object[] { primaryKey, contactPlace.getContactPlace().getUuid() });
				}
			}
		}
	}

	private void deleteOrgUnit(String uuid) {
		jdbcTemplate.update(deleteOrgUnitSQL, new Object[] { uuid, configuration.getCustomer().getCvr() });
	}

	private void deleteUser(String uuid) {
		jdbcTemplate.update(deleteUserSQL, new Object[] { uuid, configuration.getCustomer().getCvr() });
	}

	private String formatDate(Date date, boolean stopDate) {
		if (date == null) {
			return null;
		}

		if (stopDate) {
			Calendar c = Calendar.getInstance();
			c.setTime(date);

			// we run into issues with 9999-12-31 + 1 day :)
			if (c.get(Calendar.YEAR) < 9999) {
				c.add(Calendar.DATE, 1);
				date = c.getTime();
			}
		}

		return new SimpleDateFormat("yyyy-MM-dd").format(date);
	}

	private void updateLatestSTSSyncRun(Long lastRun) {
		Setting setting = settingService.getByKey(CustomerSetting.LAST_STSSYNC_RUN);
		setting.setValue(Long.toString(lastRun));
		settingService.save(setting);
	}
}
