package dk.digitalidentity.sofd.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.ModificationHistory;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.service.model.ADGridAD;
import dk.digitalidentity.sofd.service.model.ADGridAffiliation;
import dk.digitalidentity.sofd.service.model.ADGridOrgUnit;
import dk.digitalidentity.sofd.service.model.ADGridPerson;
import dk.digitalidentity.sofd.service.model.AuditWrapper;
import dk.digitalidentity.sofd.service.model.ChangeType;
import dk.digitalidentity.sofd.service.model.SyncResult;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SyncService {
	
	private static final String adGridPersonQuery =
			"SELECT person_uuid," +
			"       uuid," +
			"       cpr," +
			"       name," + 
			"       user_id," +
			"       disabled," +
			"       email," +
			"       prime," + 
			"       phone_number," +
			"       position_name," +
			"       profession_name," +
			"       pay_grade_text," + 
			"       start_date," + 
			"       orgunit_uuid," +
			"       upn," +
			"       nemlogin_user_uuid," +
			"       inherit_privileges," +
			"       kle_primary_values," + 
			"       kle_secondary_values," +
			"       user_type" +
			"  FROM view_syncservice_users" +
			"  WHERE 1=1 ";
	
	private static final String adGridAllAdQuery =
			"SELECT person_uuid," +
			"       uuid," +
			"       cpr," + 
			"       name," + 
			"       user_id," +
			"       disabled," +
			"       expired," +
			"       prime," + 
			"       email," +
			"       upn," +
			"       primary_orgunit_name," +
			"       password_expire_date," +
			"       local_extensions," +
			"       robot" +
			"  FROM view_syncservice_all_ad_users" +
			"  WHERE 1=1 ";

	private static final String adGridAllAzureQuery =
			"SELECT person_uuid," +
			"       uuid," +
			"       cpr," + 
			"       name," + 
			"       user_id," +
			"       disabled," +
			"       prime" + 
			"  FROM view_syncservice_all_azure_users";

	private static final String adGridOpusNoAdQuery =
			"SELECT person_uuid," +
					"       cpr," +
					"       name," +
					"       employee_id," +
					"       deleted" +
					"  FROM view_syncservice_all_users_with_opus_no_ad";

	private static final String adGridOrgUnitQuery =
			"SELECT uuid," + 
			"       name," + 
			"       parent_uuid," +
			"       inherit_kle," +
			"       manager_uuid," +
			"       kle_primary_values," + 
			"       kle_secondary_values" + 
			"  FROM view_syncservice_orgunits";

	private static final String managedTitlesQuery = "SELECT name, orgunit_uuid FROM managed_titles;";
	private static final String maxQuery = "SELECT IFNULL(MAX(id),0) FROM modification_history;";
	private static final String selectQuery = "SELECT id, uuid, change_type FROM modification_history WHERE id > ? and entity = ?";

	@Qualifier("defaultTemplate")
	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private SofdConfiguration configuration;

	public SyncResult getModificationHistory(Long offset, String entityType) {
		@SuppressWarnings("deprecation")
		List<ModificationHistory> modificationsList = jdbcTemplate.query(selectQuery, new Object[] { offset, entityType }, (RowMapper<ModificationHistory>) (rs, rowNum) -> {
			ModificationHistory mhObject = new ModificationHistory();
			mhObject.setId(rs.getLong("id"));
			mhObject.setChangeType(ChangeType.valueOf(rs.getString("change_type")));
			mhObject.setUuid(rs.getString("uuid"));

			return mhObject;
		});

		SyncResult result = new SyncResult();
		OptionalLong maxId = modificationsList.stream().mapToLong(ModificationHistory::getId).max();
		if (maxId.isPresent()) {
			result.setOffset(maxId.getAsLong());
		} else {
			result.setOffset(offset);
		}

		result.setUuids(modificationsList.stream().map(pc -> new AuditWrapper(pc.getUuid(), pc.getChangeType())).collect(Collectors.toList()));

		return result;
	}

	public Long getMaxOffset() {
		return jdbcTemplate.queryForObject(maxQuery, Long.class);
	}

	@SuppressWarnings("deprecation")
	public Collection<ADGridAD> getADGridAllAzure() {
		String query = adGridAllAzureQuery;
		
		return jdbcTemplate.query(query, new Object[0], (RowMapper<ADGridAD>) (rs, rowNum) -> {
			ADGridAD person = new ADGridAD();

			String personUuid = rs.getString("person_uuid");
			String uuid = rs.getString("uuid");
			String cpr = rs.getString("cpr");
			String name = rs.getString("name");
			String email = rs.getString("user_id");
			boolean prime = rs.getBoolean("prime");
			boolean disabled = rs.getBoolean("disabled");

			String userId = email;
			int idx = email.indexOf("@");
			if (idx > 0) {
				userId = email.substring(0, idx);
			}
			
			person.setPersonUuid(personUuid);
			person.setUuid(uuid);
			person.setEmail(email);
			person.setName(name);
			person.setCpr(cpr);
			person.setPrime(prime);
			person.setUserId(userId);
			person.setDisabled(disabled);

			return person;
		});
	}
	
	@SuppressWarnings("deprecation")
	public Collection<ADGridAD> getADGridAllAD() {
		String query = adGridAllAdQuery;

		if (!configuration.getIntegrations().getRoleCatalogue().isIncludeDisabled()) {
			query += " AND disabled = 0 ";
		}

		return jdbcTemplate.query(query, new Object[0], (RowMapper<ADGridAD>) (rs, rowNum) -> {
			ADGridAD person = new ADGridAD();

			String personUuid = rs.getString("person_uuid");
			String uuid = rs.getString("uuid");
			String cpr = rs.getString("cpr");
			String name = rs.getString("name");
			String userId = rs.getString("user_id");
			String email = rs.getString("email");
			String upn = rs.getString("upn");
			String primaryOrgunitName = rs.getString("primary_orgunit_name");
			String localExtensions = rs.getString("local_extensions");
			String passwordExpireDate = rs.getString("password_expire_date");
			boolean prime = rs.getBoolean("prime");
			boolean disabled = rs.getBoolean("disabled");
			boolean expired = rs.getBoolean("expired");
			boolean robot = rs.getBoolean("robot");

			person.setPersonUuid(personUuid);
			person.setUuid(uuid);
			person.setEmail(email);
			person.setName(name);
			person.setCpr(cpr);
			person.setUpn(upn);
			person.setPrimaryOrgunitName(primaryOrgunitName);
			person.setPrime(prime);
			person.setUserId(userId);
			person.setDisabled(disabled);
			person.setExpired(expired);
			person.setPasswordExpireDate(passwordExpireDate);
			person.setLocalExtensions(localExtensions);
			person.setRobot(robot);

			return person;
		});
	}

	@SuppressWarnings("deprecation")
	public Collection<ADGridAD> getADGridOpusButNoADAccount() {
		String query = adGridOpusNoAdQuery;

		return jdbcTemplate.query(query, new Object[0], (RowMapper<ADGridAD>) (rs, rowNum) -> {
			ADGridAD person = new ADGridAD();

			String personUuid = rs.getString("person_uuid");
			String cpr = rs.getString("cpr");
			String name = rs.getString("name");
			String employeeId = rs.getString("employee_id");
			boolean deleted = rs.getBoolean("deleted");

			person.setPersonUuid(personUuid);
			person.setUuid(personUuid);
			person.setName(name);
			person.setCpr(cpr);
			person.setUserId(employeeId);

			person.setDisabled(deleted);
			person.setExpired(deleted);

			return person;
		});
	}

	public Collection<ADGridPerson> getADGridPersons(boolean includeUniloginUsers, boolean includeSchoolADUsers) {
		String query = adGridPersonQuery;

		if (!configuration.getIntegrations().getRoleCatalogue().isIncludeDisabled()) {
			query += " AND disabled = 0 ";
		}
		
		LocalDate cutOff = LocalDate.now().plusDays(configuration.getIntegrations().getRoleCatalogue().getAffiliationCutoff());

		var includedUserTypes = new ArrayList<String>();
		includedUserTypes.add("'ACTIVE_DIRECTORY'");
		if( includeUniloginUsers) {
			includedUserTypes.add("'UNILOGIN'");
		}
		if( includeSchoolADUsers ) {
			includedUserTypes.add("'ACTIVE_DIRECTORY_SCHOOL'");
		}
		query += " AND user_type in (" + String.join(",", includedUserTypes) + ") ";

		@SuppressWarnings("deprecation")
		List<ADGridPerson> persons = jdbcTemplate.query(query, new Object[0], (RowMapper<ADGridPerson>) (rs, rowNum) -> {
			ADGridPerson person = new ADGridPerson();

			String personUuid = rs.getString("person_uuid");
			String uuid = rs.getString("uuid");
			String cpr = rs.getString("cpr");
			String name = rs.getString("name");
			String userId = rs.getString("user_id");
			String email = rs.getString("email");
			String upn = rs.getString("upn");
			String nemloginUserUuid = rs.getString("nemlogin_user_uuid");
			boolean prime = rs.getBoolean("prime");
			boolean disabled = rs.getBoolean("disabled");
			boolean inheritPrivileges = rs.getBoolean("inherit_privileges");
			String phoneNumber = rs.getString("phone_number");
			String positionName = rs.getString("position_name");
			String professionName = rs.getString("profession_name");
			String payGradeText = rs.getString("pay_grade_text");
			String orgUnitUuid = rs.getString("orgunit_uuid");
			String userType = rs.getString("user_type");

			String startDateStr = rs.getString("start_date");
			LocalDate startDate = LocalDate.of(1979, 5, 21);
			try {
				startDate = LocalDate.parse(startDateStr.substring(0, 10));
			}
			catch (Exception ex) {
				log.warn("Failed to parse startDate " + startDate + " on userId: " + ex.getMessage());
			}
			
			// returning null will just remove it from the dataset (by removing null entries ;))
			if (startDate.isAfter(cutOff)) {
				return null;
			}
			
			person.setSchoolUser(userType.equalsIgnoreCase("ACTIVE_DIRECTORY_SCHOOL"));
			person.setPersonUuid(personUuid);
			person.setUuid(uuid);
			person.setEmail(email);
			person.setPhone(phoneNumber);
			person.setName(name);
			person.setCpr(cpr);
			person.setUpn(upn);
			person.setNemloginUserUuid(nemloginUserUuid);
			person.setPrime(prime);
			person.setUserId(userId);
			person.setDoNotInherit(!inheritPrivileges);
			person.setDisabled(disabled);
			person.setAffiliations(new ArrayList<>());
			person.setKlePrimary(new HashSet<>());
			person.setKleSecondary(new HashSet<>());

			String klePrimary = rs.getString("kle_primary_values");
			String kleSecondary = rs.getString("kle_secondary_values");
			
			if (klePrimary != null) {
				String[] kles = klePrimary.split(",");
				person.getKlePrimary().addAll(Arrays.asList(kles));
			}
			
			if (kleSecondary != null) {
				String[] kles = kleSecondary.split(",");
				person.getKleSecondary().addAll(Arrays.asList(kles));
			}

			// TODO: bruges kun af Sønderborg, og kan nok fjernes når de kan bruge stillingskataloget i stedet
			if (configuration.getIntegrations().getRoleCatalogue().isUsePayGradeText() && StringUtils.hasText(payGradeText) && StringUtils.hasText(orgUnitUuid)) {
				ADGridAffiliation affiliation = new ADGridAffiliation();
				affiliation.setOrgUnitUuid(orgUnitUuid);
				affiliation.setPositionName(payGradeText);
				affiliation.setProfessionName(professionName);
				affiliation.setDoNotInherit(!inheritPrivileges);

				person.getAffiliations().add(affiliation);
			}
			else if (StringUtils.hasLength(positionName) && StringUtils.hasLength(orgUnitUuid)) {
				ADGridAffiliation affiliation = new ADGridAffiliation();
				affiliation.setOrgUnitUuid(orgUnitUuid);
				affiliation.setPositionName(positionName);
				affiliation.setProfessionName(professionName);
				affiliation.setDoNotInherit(!inheritPrivileges);

				person.getAffiliations().add(affiliation);
			}

			return person;
		});
		
		// remove null entries
		persons.removeAll(Collections.singletonList(null));
		
		List<User> mitIdErhvervUsers = userService.findByUserTypeAndDisabledFalse(SupportedUserTypeService.getMitIDErhvervUserType());
		
		// we only want those with the special "mitid-" masterId prefix, as those are not handled by the SQL VIEW extration
		// but we want to map it to an easy lookup map for later use
		Map<String, User> mitIdErhvervLookupMap = new HashMap<String, User>();
		for (User user : mitIdErhvervUsers.stream().filter(u -> u.getMasterId().startsWith("mitid-")).collect(Collectors.toList())) {
			String key = user.getMasterId().substring("mitid-".length());
			mitIdErhvervLookupMap.put(key, user);
		}
		
		Map<String, ADGridPerson> result = new HashMap<>();
		for (ADGridPerson person : persons) {
			String key = person.getUuid() + person.getUserId();

			if (result.containsKey(key)) {
				ADGridPerson p = result.get(key);

				p.getAffiliations().addAll(person.getAffiliations());
				p.getKlePrimary().addAll(person.getKlePrimary());
				p.getKleSecondary().addAll(person.getKleSecondary());

				// if just one of them can inherit privileges, all of them can
				if (!p.isDoNotInherit()) {
					p.setDoNotInherit(false);
				}
			}
			else {
				// first time we see this user - check for MitID Erhverv and map MitID UUID into person if available
				User user = mitIdErhvervLookupMap.get(person.getUserId());
				if (user != null) {
					person.setNemloginUserUuid(user.getUserId());
				}
				
				result.put(key, person);
			}
		}
		
		return result.values();
	}
	
	class ADGridOrgUnitTitle {
		String orgUnitUuid;
		String name;
	}
	
	public Collection<ADGridOrgUnit> getADGridOrgUnits() {
		List<ADGridOrgUnitTitle> titles = jdbcTemplate.query(managedTitlesQuery, (RowMapper<ADGridOrgUnitTitle>) (rs, rowNum) -> {
			ADGridOrgUnitTitle title = new ADGridOrgUnitTitle();
			
			String orgUnitUuid = rs.getString("orgunit_uuid");
			String name = rs.getString("name");
			
			title.orgUnitUuid = orgUnitUuid;
			title.name = name;

			return title;
		});

		@SuppressWarnings("deprecation")
		List<ADGridOrgUnit> orgUnits = jdbcTemplate.query(adGridOrgUnitQuery, new Object[0], (RowMapper<ADGridOrgUnit>) (rs, rowNum) -> {
			ADGridOrgUnit orgUnit = new ADGridOrgUnit();
			
			String uuid = rs.getString("uuid");
			String name = rs.getString("name");
			String parentUuid = rs.getString("parent_uuid");
			boolean inheritKle = rs.getBoolean("inherit_kle");
			String managerUuid = rs.getString("manager_uuid");
			String klePrimary = rs.getString("kle_primary_values");
			String kleSecondary = rs.getString("kle_secondary_values");

			orgUnit.setUuid(uuid);
			orgUnit.setName(name);
			orgUnit.setParentUuid(parentUuid);
			orgUnit.setInheritKle(inheritKle);
			orgUnit.setManagerUuid(managerUuid);
			orgUnit.setKlePrimary(new HashSet<>());
			orgUnit.setKleSecondary(new HashSet<>());
			orgUnit.setTitles(new HashSet<>());
			
			if (klePrimary != null) {
				String[] kles = klePrimary.split(",");
				orgUnit.getKlePrimary().addAll(Arrays.asList(kles));
			}
			
			if (kleSecondary != null) {
				String[] kles = kleSecondary.split(",");
				orgUnit.getKleSecondary().addAll(Arrays.asList(kles));
			}

			for (ADGridOrgUnitTitle title : titles) {
				if (Objects.equals(title.orgUnitUuid, uuid)) {
					orgUnit.getTitles().add(title.name);
				}
			}
			
			return orgUnit;
		});

		return orgUnits;
	}
}
