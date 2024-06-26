package dk.digitalidentity.sofd.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import dk.digitalidentity.sofd.service.model.ADGridAD;
import dk.digitalidentity.sofd.service.model.ADGridAffiliation;
import dk.digitalidentity.sofd.service.model.ADGridOrgUnit;
import dk.digitalidentity.sofd.service.model.ADGridPerson;
import dk.digitalidentity.sofd.service.model.AuditWrapper;
import dk.digitalidentity.sofd.service.model.ChangeType;
import dk.digitalidentity.sofd.service.model.SyncResult;

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
			"       local_extensions" +
			"  FROM view_syncservice_all_ad_users";

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
	public Collection<ADGridAD> getADGridAllAD() {
		String query = adGridAllAdQuery;
		
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
			boolean prime = rs.getBoolean("prime");
			boolean disabled = rs.getBoolean("disabled");
			boolean expired = rs.getBoolean("expired");

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
			person.setLocalExtensions(localExtensions);

			return person;
		});
	}

	public Collection<ADGridPerson> getADGridPersons(boolean includeUniloginUsers, boolean includeSchoolADUsers) {
		String query = adGridPersonQuery;


		if(!configuration.getIntegrations().getRoleCatalogue().isIncludeDisabled()) {
			query += " AND disabled = 0 ";
		}

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
			String orgUnitUuid = rs.getString("orgunit_uuid");
			String userType = rs.getString("user_type");

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

			if (StringUtils.hasLength(positionName) && StringUtils.hasLength(orgUnitUuid)) {
				ADGridAffiliation affiliation = new ADGridAffiliation();
				affiliation.setOrgUnitUuid(orgUnitUuid);
				affiliation.setPositionName(positionName);
				affiliation.setDoNotInherit(!inheritPrivileges);

				person.getAffiliations().add(affiliation);
			}

			return person;
		});
		
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
