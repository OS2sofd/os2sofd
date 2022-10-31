package dk.digitalidentity.sofd.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.ContactPlace;
import dk.digitalidentity.sofd.dao.model.Email;
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
import dk.digitalidentity.sofd.dao.model.mapping.ContactPlaceOrgUnitMapping;
import dk.digitalidentity.sofd.service.model.SyncResult;
import dk.digitalidentity.sofd.service.os2sync.dto.FKOU;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OS2SyncService {
	private static final String updateUserSQL = "INSERT INTO queue_users (uuid, user_id, phone_number, email, racfid, name, cpr, landline, cvr, operation) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'UPDATE');";
	private static final String deleteUserSQL = "INSERT INTO queue_users (uuid, cvr, operation) VALUES (?, ?, 'DELETE');";
	private static final String updatePositionSQL = "INSERT INTO queue_user_positions (user_id, name, orgunit_uuid, start_date, stop_date) VALUES (?, ?, ?, ?, ?);";
	private static final String updateOrgUnitSQL = "INSERT INTO queue_orgunits (uuid, name, parent_ou_uuid, los_shortname, phone_number, email, ean, post_address, orgunit_type, contact_open_hours, landline, cvr, operation, location, url, email_remarks, post_return, phone_open_hours) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'UPDATE', ?, ?, ?, ?, ?);";
	private static final String deleteOrgUnitSQL = "INSERT INTO queue_orgunits (uuid, cvr, operation) VALUES (?, ?, 'DELETE');";
	private static final String updateOrgUnitTaskSQL = "INSERT INTO queue_orgunits_tasks (unit_id, task) VALUES (?, ?);";
	private static final String updateOrgUnitTagSQL = "INSERT INTO queue_orgunits_it_systems (unit_id, it_system_uuid) VALUES (?, ?);";
	private static final String updateOrgUnitContactPlaceSQL = "INSERT INTO queue_orgunits_contact_for_tasks (unit_id, task) VALUES (?, ?);";
	private static final String updateOrgUnitContactPlaceUsersSQL = "INSERT INTO queue_orgunits_contact_places (unit_id, contact_place_uuid) VALUES (?, ?);";

	@Autowired
	private SofdConfiguration configuration;

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
	
	@Qualifier("OS2syncTemplate")
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Transactional
	public void synchronizeHierarchy() {
		if (!configuration.getScheduled().isEnabled() || !configuration.getIntegrations().getOs2sync().isEnabled()) {
			log.info("OS2sync synchronization disabled on this instance");
			return;
		}

		log.info("Peforming cleanup of orgunits in FK Organisation");

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
		List<OrgUnit> sofdOrgUnits = orgUnitService.getAll().stream().filter(o -> o.isDeleted() == false).collect(Collectors.toList());

		// read contactPlaces
		List<ContactPlace> allContactPlaces = (configuration.getModules().getContactPlaces().isEnabled()) ? contactPlaceService.findAll() : null;
		
		// find all OrgUnits (in SOFD) that are not currently in FK Organisation and trigger a sync on those through OS2sync
		for (OrgUnit orgUnit : sofdOrgUnits) {
			if (fkOrgUnits.stream().noneMatch(fkou -> Objects.equals(fkou.getUuid(), orgUnit.getUuid()))) {
				log.info("Weekly sync found OU not present in FK Organisation: " + orgUnit.getName() + " / " + orgUnit.getUuid());
				updateOrgUnit(orgUnit, allContactPlaces);
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
	public void fullUpdate() {
		Long head = syncService.getMaxOffset();
		if (head == null) {
			head = 0L;
		}

		List<ContactPlace> allContactPlaces = (configuration.getModules().getContactPlaces().isEnabled()) ? contactPlaceService.findAll() : null;

		List<OrgUnit> orgUnits = orgUnitService.getAll();
		for (OrgUnit orgUnit : orgUnits) {
			if (orgUnit.isDeleted()) {
				deleteOrgUnit(orgUnit.getUuid());
			}
			else {
				updateOrgUnit(orgUnit, allContactPlaces);
			}
		}

		List<Person> persons = personService.getAll();
		for (Person person : persons) {
			if (person.isDeleted()) {
				deleteUser(person.getUuid());
			}
			else {
				updateUser(person);
			}
		}

		updateLatestSTSSyncRun(head);
	}

	@Transactional
	public void deltaUpdate(Long lastRun) {
		Long updatedLastRun = lastRun;

		SyncResult syncResult = syncService.getModificationHistory(lastRun, EntityType.ORGUNIT.toString());
		Set<String> uuids = syncResult.getUuids().stream().map(w -> w.getUuid()).collect(Collectors.toSet());

		List<ContactPlace> allContactPlaces = null;
		if (uuids.size() > 0 && configuration.getModules().getContactPlaces().isEnabled()) {
			allContactPlaces = contactPlaceService.findAll();
		}

		for (String uuid : uuids) {
			OrgUnit orgUnit = orgUnitService.getByUuid(uuid);

			if (orgUnit == null || orgUnit.isDeleted()) {
				deleteOrgUnit(uuid);
			}
			else {
				updateOrgUnit(orgUnit, allContactPlaces);
				
				// if the orgunit has inherit KLE flagged, we ALSO need to sync all children (and childrens children)
				if (orgUnit.isInheritKle()) {
					updateChildren(orgUnit, uuids, allContactPlaces);
				}
			}
		}

		if (syncResult.getOffset() > updatedLastRun) {
			updatedLastRun = syncResult.getOffset();
		}

		syncResult = syncService.getModificationHistory(lastRun, EntityType.PERSON.toString());
		uuids = syncResult.getUuids().stream().map(w -> w.getUuid()).collect(Collectors.toSet());

		for (String uuid : uuids) {
			Person person = personService.getByUuid(uuid);

			if (person.isDeleted()) {
				deleteUser(uuid);
			}
			else {
				updateUser(person);
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
						updateOrgUnit(orgUnit, allContactPlaces);
					}
				}
				
				for (OrgUnit orgUnit : mapUsers.values()) {
					if (!orgUnit.isDeleted()) {
						updateOrgUnit(orgUnit, allContactPlaces);
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

	private void updateChildren(OrgUnit orgUnit, Set<String> uuids, List<ContactPlace> contactPlaces) {
		if (orgUnit.getChildren() == null) {
			return;
		}
		
		for (OrgUnit child : orgUnit.getChildren()) {
			if (child.isDeleted()) {
				continue;
			}

			// no reason to update the child if already have this scheduled
			if (!uuids.contains(child.getUuid())) {
				updateOrgUnit(child, contactPlaces);
			}

			updateChildren(child, uuids, contactPlaces);
		}
	}

	public void deleteOrgUnit(String uuid) {
		jdbcTemplate.update(deleteOrgUnitSQL, new Object[] { uuid, configuration.getCustomer().getCvr() });
	}
	
	public void deleteUser(String uuid) {
		jdbcTemplate.update(deleteUserSQL, new Object[] { uuid, configuration.getCustomer().getCvr() });
	}

	public void updateUser(Person person) {

		// find mobile phone number
		Optional<Phone> phone = PersonService.getPhones(person).stream().filter(p -> p.isTypePrime() && p.getPhoneType().equals(PhoneType.MOBILE)).findFirst();
		final String phoneValue = phone.isPresent() ? phone.get().getPhoneNumber() : null;
		
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

		// find prime email
		Optional<User> emailUser = PersonService.getUsers(person).stream().filter(u -> SupportedUserTypeService.isExchange(u.getUserType()) && u.isPrime()).findFirst();
		final String emailValue = (emailUser.isPresent()) ? emailUser.get().getUserId() : null;

		// find prime kspCics
		Optional<User> kspCicsUser = PersonService.getUsers(person).stream().filter(u -> SupportedUserTypeService.isKspCics(u.getUserType()) && u.isPrime()).findFirst();
		final String kspCicsValue = (kspCicsUser.isPresent()) ? kspCicsUser.get().getUserId() : null;

		// find name
		final String nameValue = (person.getChosenName() != null) ? person.getChosenName() : (person.getFirstname() + " " + person.getSurname());

		// find cpr number
		final String cprValue = (configuration.getIntegrations().getOs2sync().isCprEnabled()) ? person.getCpr() : null;

		// find user_id
		Optional<User> adUser = PersonService.getUsers(person).stream().filter(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType()) && u.isPrime()).findFirst();
		final String userId = (adUser.isPresent()) ? adUser.get().getUserId() : null;

		if (userId == null) {
			log.debug("Could not synchronize person without Active Directory account: " + person.getUuid());
			return;
		}

		if (person.getAffiliations().size() == 0) {
			log.debug("Could not synchronize person without Affiliation: " + person.getUuid());
			return;
		}

		GeneratedKeyHolder holder = new GeneratedKeyHolder();
		jdbcTemplate.update(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement statement = con.prepareStatement(updateUserSQL, Statement.RETURN_GENERATED_KEYS);
				statement.setString(1, person.getUuid());
				statement.setString(2, userId);
				statement.setString(3, phoneValue);
				statement.setString(4, emailValue);
				statement.setString(5, kspCicsValue);
				statement.setString(6, nameValue);
				statement.setString(7, cprValue);
				statement.setString(8, landlineValue);
				statement.setString(9, configuration.getCustomer().getCvr());

				return statement;
			}
		}, holder);

		final Long primaryKey = holder.getKey().longValue();

		AffiliationService.notStoppedAffiliations(person.getAffiliations()).stream()
				.forEach(a -> jdbcTemplate.update(updatePositionSQL, new Object[] { primaryKey, a.getPositionName(), a.getOrgUnit().getUuid(), formatDate(a.getStartDate()), formatDate(a.getStopDate()) }));
	}
	
	private void updateOrgUnit(OrgUnit orgUnit, List<ContactPlace> contactPlaces) {

		// find name
		final String nameValue = orgUnit.getName();

		// find parent
		final String parentUuid = (orgUnit.getParent() != null) ? orgUnit.getParent().getUuid() : null;

		// find LOS shortkey
		final String losValue = orgUnit.getShortname();

		// find mobile phone number
		Optional<Phone> phone = OrgUnitService.getPhones(orgUnit).stream().filter(p -> p.isTypePrime() && p.getPhoneType().equals(PhoneType.MOBILE)).findFirst();
		String phoneValueTemp = null;
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
		final String phoneValue = phoneValueTemp;
		
		// find landline phone number
		Optional<Phone> landline = OrgUnitService.getPhones(orgUnit).stream().filter(p -> p.isTypePrime() && p.getPhoneType().equals(PhoneType.LANDLINE)).findFirst();
		String landlineValueTemp = null;
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
		final String landlineValue = landlineValueTemp;

		// find prime email
		Optional<Email> email = OrgUnitService.getEmails(orgUnit).stream().filter(m -> m.isPrime()).findFirst();
		final String emailValue = email.isPresent() ? email.get().getEmail() : null;

		// find EAN
		final String eanValue = (orgUnit.getEan() != null) ? Long.toString(orgUnit.getEan()) : null;

		// find KLE
		List<String> tasks = new ArrayList<>();
		for (String code : orgUnitService.getKleCodesIncludingInherited(orgUnit)) {
			Kle kle = kleService.getByCode(code);
			if (kle != null && StringUtils.hasLength(kle.getUuid())) {
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
		if (StringUtils.hasLength(orgUnit.getOpeningHours())) {
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
		final String location = orgUnit.getLocation();
		// find url
		final String url = orgUnit.getUrlAddress();
		// find email notes
		final String emailNotes = orgUnit.getEmailNotes();
		
		// find return address
		Optional<Post> returnPost = OrgUnitService.getPosts(orgUnit).stream().filter(p -> p.isReturnAddress()).findFirst();
		String tempReturnPost = null;
		if (returnPost.isPresent()) {
			Post p = returnPost.get();
			tempReturnPost = p.getAsOneLine();
		}
		final String returnAddress = tempReturnPost;

		// find phone opening hours
		final String openingHoursPhone = orgUnit.getOpeningHoursPhone();

		GeneratedKeyHolder holder = new GeneratedKeyHolder();
		jdbcTemplate.update(new PreparedStatementCreator() {

			@Override
			public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
				PreparedStatement statement = con.prepareStatement(updateOrgUnitSQL, Statement.RETURN_GENERATED_KEYS);
				statement.setString(1, orgUnit.getUuid());
				statement.setString(2, nameValue);
				statement.setString(3, parentUuid);
				statement.setString(4, losValue);
				statement.setString(5, phoneValue);
				statement.setString(6, emailValue);
				statement.setString(7, eanValue);
				statement.setString(8, postValue);
				statement.setString(9, orgUnitType);
				statement.setString(10, contactOpenHours);
				statement.setString(11, landlineValue);
				statement.setString(12, configuration.getCustomer().getCvr());
				statement.setString(13, location);
				statement.setString(14, url);
				statement.setString(15, emailNotes);
				statement.setString(16, returnAddress);
				statement.setString(17, openingHoursPhone);

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

	private String formatDate(Date date) {
		if (date == null) {
			return null;
		}

		return new SimpleDateFormat("yyyy-MM-dd").format(date);
	}
	
	private void updateLatestSTSSyncRun(Long lastRun) {
		Setting setting = settingService.getByKey(CustomerSetting.LAST_STSSYNC_RUN);
		setting.setValue(Long.toString(lastRun));
		settingService.save(setting);
	}
}
