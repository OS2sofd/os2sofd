package dk.digitalidentity.sofd.service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.dto.ADUserReportDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.ActiveAffiliationOrActiveADAccountReportDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.MultipleAffiliationsReportDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.PersonWithActiveSOFDAffiliationsReportDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.PersonWithAffiliationsWorkplacesReportDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.SofdAffiliationsReportDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.enums.ADUserStatus;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.Workplace;

@Service
public class ReportService {
	
	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private PersonService personService;

	@Qualifier("defaultTemplate")
	@Autowired
	private JdbcTemplate jdbcTemplate;

	/*
		SELECT MIN(id) AS rev_id
		FROM revisions
		WHERE timestamp > (UNIX_TIMESTAMP(?) * 1000);
	 */
	private static final String SELECT_MIN_REV = "SELECT MIN(id) AS rev_id FROM revisions WHERE timestamp > (UNIX_TIMESTAMP('?') * 1000);";

	/*
        SELECT user_id, person_uuid, MAX(rev) AS lastMatchRev FROM persons_users_aud
          WHERE user_id IS NOT NULL
          GROUP BY user_id, person_uuid;
	 */
	private static final String SELECT_USER_PERSON_MAPPING = "SELECT user_id, person_uuid, MAX(rev) AS lastMatchRev FROM persons_users_aud WHERE user_id IS NOT NULL GROUP BY user_id, person_uuid;";

	/*
		SELECT s.user_id, s.id, s.when_created AS created_time, FROM_UNIXTIME(r2.timestamp / 1000) AS disabled_time
		FROM (SELECT du.id, du.user_id, ad.when_created, dr.disabledRev
		  FROM users du
		  JOIN active_directory_details ad ON ad.user_id = du.id
		  INNER JOIN (SELECT id, MAX(rev) AS disabledRev FROM users_aud WHERE disabled = 1 and rev >= 1 GROUP BY id) dr ON dr.id = du.id
		  WHERE du.disabled = 1 AND du.user_type = 'ACTIVE_DIRECTORY') s
		JOIN revisions r2 ON r2.id = s.disabledRev;
	 */
	private static final String SELECT_DISABLED_USERS = "SELECT s.user_id, s.id, s.when_created AS created_time, FROM_UNIXTIME(r2.timestamp / 1000) AS disabled_time FROM (SELECT du.id, du.user_id, ad.when_created, dr.disabledRev FROM users du JOIN active_directory_details ad ON ad.user_id = du.id INNER JOIN (SELECT id, MAX(rev) AS disabledRev FROM users_aud WHERE disabled = 1 and rev >= 1 GROUP BY id) dr ON dr.id = du.id WHERE du.disabled = 1 AND du.user_type = 'ACTIVE_DIRECTORY') s JOIN revisions r2 ON r2.id = s.disabledRev;";

	/*
		SELECT lnd.user_id, lnd.id, ad.when_created, FROM_UNIXTIME(r.timestamp / 1000) AS deleted_time
		FROM (SELECT lastNonDeleted.rev, lastNonDeleted.id, lastNonDeleted.user_id FROM (
			SELECT MAX(rev) AS rev, id, user_id
  				FROM users_aud
  				WHERE user_type = 'ACTIVE_DIRECTORY'
  				GROUP BY user_id) lastNonDeleted
			) lnd
		INNER JOIN users_aud uad ON uad.id = lnd.id
		LEFT JOIN (SELECT adDetails.when_created, adDetails.user_id FROM (
			SELECT MAX(rev), user_id, when_created
			FROM active_directory_details_aud
			WHERE when_created IS NOT NULL
			GROUP BY id) adDetails
		) ad ON ad.user_id = lnd.id
		JOIN revisions r ON r.id = uad.rev
		WHERE uad.rev > ?
		AND uad.revtype = 2;
	 */
	private static final String SELECT_DELETED_USERS = "SELECT lnd.user_id, lnd.id, ad.when_created, FROM_UNIXTIME(r.timestamp / 1000) AS deleted_time FROM (SELECT lastNonDeleted.rev, lastNonDeleted.id, lastNonDeleted.user_id FROM (SELECT MAX(rev) AS rev, id, user_id FROM users_aud WHERE user_type = 'ACTIVE_DIRECTORY' GROUP BY user_id) lastNonDeleted) lnd INNER JOIN users_aud uad ON uad.id = lnd.id LEFT JOIN (SELECT adDetails.when_created, adDetails.user_id FROM (SELECT MAX(rev), user_id, when_created FROM active_directory_details_aud WHERE when_created IS NOT NULL GROUP BY id) adDetails) ad ON ad.user_id = lnd.id JOIN revisions r ON r.id = uad.rev WHERE uad.rev > ? AND uad.revtype = 2;";

	/*
		SELECT u.user_id, u.id, ad.when_created
		FROM users u
		JOIN active_directory_details ad ON ad.user_id = u.id
		WHERE u.user_type = 'ACTIVE_DIRECTORY'
		AND u.disabled = 0;
	 */
	private static final String SELECT_ACTIVE_USERS = "SELECT u.user_id, u.id, ad.when_created FROM users u JOIN active_directory_details ad ON ad.user_id = u.id WHERE u.user_type = 'ACTIVE_DIRECTORY' AND u.disabled = 0;";

	public List<Person> generateOpusButNoADReport() {
		List<Person> persons = personService.getActiveCached().stream()
				// Persons that have OPUS account
				.filter(p -> PersonService.getUsers(p).stream().anyMatch(u -> SupportedUserTypeService.isOpus(u.getUserType()) && u.isDisabled() == false))
				// Persons without AD account
				.filter(p -> PersonService.getUsers(p).stream().noneMatch(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType()) && u.isDisabled() == false))
				.collect(Collectors.toList());

		return persons;
	}

	public List<Person> generateADWithoutAffiliationReport() {
		List<Person> persons = personService.getActiveCached().stream()
				// Persons that have AD account (non-substitute)
				.filter(p -> PersonService.getUsers(p).stream().anyMatch(u ->
					SupportedUserTypeService.isActiveDirectory(u.getUserType()) &&
					u.isDisabled() == false &&
					!UserService.isSubstituteUser(u)
				))

				// Persons without affiliations or persons that have all deleted or stopped affiliations
				.filter(p -> p.getAffiliations().isEmpty() ||
						p.getAffiliations().stream().allMatch(af -> (af.isDeleted()) || (AffiliationService.notActiveAnymore(af)))
				)
				.collect(Collectors.toList());

		return persons;
	}
	
	public List<Person> generateADWithoutActiveOpusAffiliationReport() {
		List<Person> persons = personService.getActiveCached().stream()
				// Persons that have AD account (non-substitute)
				.filter(p -> PersonService.getUsers(p).stream().anyMatch(u ->
					SupportedUserTypeService.isActiveDirectory(u.getUserType()) &&
					u.isDisabled() == false &&
					!UserService.isSubstituteUser(u)
				))

				// Persons without affiliations or persons that have all deleted or stopped affiliations (or non OPUS affiliations)
				.filter(p -> p.getAffiliations().stream().allMatch(af -> (!configuration.getModules().getLos().getPrimeAffiliationMaster().equals(af.getMaster()) || af.isDeleted()) || (AffiliationService.notActiveAnymore(af)))
				)
				.collect(Collectors.toList());

		return persons;
	}
	
	public List<Person> generateAccountOrdersDisabledReport() {
		List<Person> persons = personService.getActiveCached().stream()
				.filter(p -> p.isDisableAccountOrdersCreate() || p.isDisableAccountOrdersDisable() || p.isDisableAccountOrdersDelete())
				.collect(Collectors.toList());

		return persons;
	}
	
	public List<Person> generatePersonsForceStopReport() {
		List<Person> persons = personService.getActiveCached().stream()
				.filter(p -> p.isForceStop())
				.collect(Collectors.toList());

		return persons;
	}

	public List<Person> generatePersonsOnLeaveReport() {
		List<Person> persons = personService.getActiveCached().stream()
				.filter(p -> p.getLeave() != null)
				.collect(Collectors.toList());

		return persons;
	}

	public List<Person> generateDuplicateAffiliationReport() {
		List<Person> persons = new ArrayList<Person>();

		for (Person person : personService.getActiveCached()) {
			Set<String> orgUnits = new HashSet<>();

			var activeAffiliations = AffiliationService.onlyActiveAffiliations(person.getAffiliations());
			// find all SOFD affiliations
			for (Affiliation  affiliation : activeAffiliations) {
				if (affiliation.getMaster().equals("SOFD")) {
					orgUnits.add(affiliation.getCalculatedOrgUnit().getUuid());
				}
			}

			// find non-SOFD affiliations that maps to same OrgUnit as a SOFD-owned affiliation
			for (Affiliation  affiliation : activeAffiliations) {
				if (!affiliation.getMaster().equals("SOFD")) {
					if (orgUnits.contains(affiliation.getCalculatedOrgUnit().getUuid())) {
						persons.add(person);
						break;
					}
				}
			}
		}

		return persons;
	}
	
	public List<MultipleAffiliationsReportDTO> generateMultipleAffiliationsReport() {
		List<MultipleAffiliationsReportDTO> multipleAffiliationsReportDTOs = new ArrayList<>();
		String wagesSystemMaster = configuration.getModules().getLos().getPrimeAffiliationMaster();

		for (Person person : personService.getActiveCached()) {

			// filter user accounts
			List<User> users = PersonService.getUsers(person).stream()
					.filter(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType()) && u.isDisabled() == false)
					.collect(Collectors.toList());

			// filter affiliations
			List<Affiliation> affiliations = AffiliationService.notStoppedAffiliations(person.getAffiliations())
					.stream()
					.filter(a -> a.getMaster().equals(wagesSystemMaster))
					.collect(Collectors.toList());

			// at least one AD account and at least two "OPUS" affiliations
			if (users.size() >= 1 && affiliations.size() >= 2) {
				for (Affiliation affiliation : affiliations) {
					for (User user : users) {
						if (user.getEmployeeId() == null || user.getEmployeeId().equals(affiliation.getEmployeeId())) {
							MultipleAffiliationsReportDTO dto = new MultipleAffiliationsReportDTO();
							dto.setUuid(person.getUuid());
							dto.setName(PersonService.getName(person));
							dto.setCpr(PersonService.maskCpr(person.getCpr()));
							dto.setAffiliationName(AffiliationService.getPositionName(affiliation));
							dto.setAffiliationOrgUnitName(affiliation.getCalculatedOrgUnit().getName());
							dto.setPrimeAffiliation(affiliation.isPrime());
							dto.setAffiliationTerms(affiliation.getEmploymentTermsText());
							dto.setEmployeeId(affiliation.getEmployeeId());
							dto.setUserId(user.getUserId());

							multipleAffiliationsReportDTOs.add(dto);
						}
					}
				}
			}
		}

		return multipleAffiliationsReportDTOs;
	}
	
	public List<SofdAffiliationsReportDTO> generateSofdAffiliationsReport() {
		List<SofdAffiliationsReportDTO> sofdAffiliationsReportDTO = new ArrayList<>();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

		for (Person person : personService.getActiveCached()) {

			// filter affiliations
			List<Affiliation> affiliations = AffiliationService.notStoppedAffiliations(person.getAffiliations())
					.stream()
					.filter(a -> a.getMaster().equals("SOFD"))
					.collect(Collectors.toList());
			
			List<Affiliation> nonSofdAffiliations = AffiliationService.notStoppedAffiliations(person.getAffiliations())
					.stream()
					.filter(a -> !a.getMaster().equals("SOFD"))
					.collect(Collectors.toList());

			// at least one not-stopped SOFD affiliation, and no other non-stopped affiliations
			if (affiliations.size() >= 1 && nonSofdAffiliations.size() == 0) {
				for (Affiliation affiliation : affiliations) {
					SofdAffiliationsReportDTO dto = new SofdAffiliationsReportDTO();
					dto.setUuid(person.getUuid());
					dto.setName(PersonService.getName(person));
					dto.setCpr(PersonService.maskCpr(person.getCpr()));
					dto.setAffiliationName(AffiliationService.getPositionName(affiliation));
					dto.setAffiliationOrgUnitName(affiliation.getCalculatedOrgUnit().getName());
					dto.setAffiliationVendor(affiliation.getVendor());
					dto.setAffiliationStartDate(affiliation.getStartDate() != null ? sdf.format(affiliation.getStartDate()) : "");
					dto.setAffiliationStopDate(affiliation.getStopDate() != null ? sdf.format(affiliation.getStopDate()) : "");

					sofdAffiliationsReportDTO.add(dto);
				}
			}
		}

		return sofdAffiliationsReportDTO;
	}

	record PersonUserMapping(String personUuid, long userId) {}
	public List<ADUserReportDTO> generateADUsersReport(LocalDate date) throws Exception {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		List<ADUserReportDTO> result = new ArrayList<>();
		
		// find min rev id that we are using in the next queries (DON'T DO THIS AT HOME KIDS ... SQL INJECTION if you do thing wrong...)
		String query = SELECT_MIN_REV.replace("?", date.format(formatter));
		@SuppressWarnings("deprecation")
		List<Long> minRevResult = jdbcTemplate.query(query, new Object[] { }, (RowMapper<Long>) (rs, rownum) -> {
			return rs.getLong("rev_id");
		});
	
		if (minRevResult == null || minRevResult.isEmpty()) {
			throw new Exception("Failed to get min rev id when creating rows for AD user report");
		}
		long minRevId = minRevResult.get(0);

		// find the person user mappings
		@SuppressWarnings("deprecation")
		List<PersonUserMapping> personUserMappings = jdbcTemplate.query(SELECT_USER_PERSON_MAPPING, new Object[] { }, (RowMapper<PersonUserMapping>) (rs, rownum) -> {
			return new PersonUserMapping(rs.getString("person_uuid"), rs.getLong("user_id"));
		});

		if (personUserMappings == null) {
			throw new Exception("Failed to get person user mappings when creating rows for AD user report");
		}

		Map<Long, String> userIdToPersonMap = personUserMappings.stream().collect(Collectors.toMap(PersonUserMapping::userId, PersonUserMapping::personUuid));
		
		// find all persons (including deleted = true)
		List<Person> allPersons = personService.getAll();
		Map<String, Person> personMap = allPersons.stream().collect(Collectors.toMap(Person::getUuid, Function.identity())); 
		Set<String> alreadyFoundUsers = new HashSet<>();
		
		// create the rows for active
		@SuppressWarnings("deprecation")
		List<ADUserReportDTO> activeDtos = jdbcTemplate.query(SELECT_ACTIVE_USERS, new Object[] {}, (RowMapper<ADUserReportDTO>) (rs, rownum) -> {
			ADUserReportDTO dto = new ADUserReportDTO();

			// find person if exists
			long idOfUser = rs.getLong("id");

			String personUuid = userIdToPersonMap.get(idOfUser);
			if (personUuid != null) {
				Person person = personMap.get(personUuid);

				if (person != null) {
					dto.setPersonUuid(person.getUuid());
					dto.setName(PersonService.getName(person));
				}
			}

			dto.setUserId(rs.getString("user_id"));
			dto.setStatus(ADUserStatus.ACTIVE);
			
			String createdString = rs.getString("when_created");
			if (StringUtils.hasLength(createdString) && createdString.length() >= 10) {
				dto.setCreated(LocalDate.parse(createdString.substring(0, 10), formatter));
			}

			// keep track of all added, to avoid duplicates
			alreadyFoundUsers.add(dto.getUserId().toLowerCase());
			
			return dto;
		});

		result.addAll(activeDtos);

		// create the rows for disabled
		@SuppressWarnings("deprecation")
		List<ADUserReportDTO> disabledDtos = jdbcTemplate.query(SELECT_DISABLED_USERS, new Object[] {}, (RowMapper<ADUserReportDTO>) (rs, rownum) -> {
			ADUserReportDTO dto = new ADUserReportDTO();

			// find person if exists
			long idOfUser = rs.getLong("id");
			
			String personUuid = userIdToPersonMap.get(idOfUser);
			if (personUuid != null) {
				Person person = personMap.get(personUuid);

				if (person != null) {
					dto.setPersonUuid(person.getUuid());
					dto.setName(PersonService.getName(person));
				}
			}

			dto.setUserId(rs.getString("user_id"));
			dto.setStatus(ADUserStatus.CLOSED);
			String createdString = rs.getString("created_time");
			if (StringUtils.hasLength(createdString) && createdString.length() >= 10) {
				dto.setCreated(LocalDate.parse(createdString.substring(0, 10), formatter));
			}

			String disabledString = rs.getString("disabled_time");
			if (StringUtils.hasLength(disabledString) && disabledString.length() >= 10) {
				dto.setClosed(LocalDate.parse(disabledString.substring(0, 10), formatter));
			}

			return dto;
		});

		// avoid duplicates
		for (ADUserReportDTO dto : disabledDtos) {
			if (alreadyFoundUsers.contains(dto.getUserId().toLowerCase())) {
				continue;
			}
			
			result.add(dto);

			// keep track of all added, to avoid duplicates
			alreadyFoundUsers.add(dto.getUserId().toLowerCase());
		}

		// create the rows for deleted
		@SuppressWarnings("deprecation")
		List<ADUserReportDTO> deletedDtos = jdbcTemplate.query(SELECT_DELETED_USERS, new Object[] {minRevId}, (RowMapper<ADUserReportDTO>) (rs, rownum) -> {
			ADUserReportDTO dto = new ADUserReportDTO();

			// find person if exists
			long idOfUser = rs.getLong("id");

			String personUuid = userIdToPersonMap.get(idOfUser);
			if (personUuid != null) {
				Person person = personMap.get(personUuid);

				if (person != null) {
					dto.setPersonUuid(person.getUuid());
					dto.setName(PersonService.getName(person));
				}
			}

			dto.setUserId(rs.getString("user_id"));
			dto.setStatus(ADUserStatus.CLOSED);
			String createdString = rs.getString("when_created");
			if (StringUtils.hasLength(createdString) && createdString.length() >= 10) {
				dto.setCreated(LocalDate.parse(createdString.substring(0, 10), formatter));
			}

			String deletedString = rs.getString("deleted_time");
			if (StringUtils.hasLength(deletedString) && deletedString.length() >= 10) {
				dto.setClosed(LocalDate.parse(deletedString.substring(0, 10), formatter));
			}

			return dto;
		});
		
		// avoid duplicates
		for (ADUserReportDTO dto : deletedDtos) {
			if (alreadyFoundUsers.contains(dto.getUserId().toLowerCase())) {
				continue;
			}
			
			result.add(dto);

			// keep track of all added, to avoid duplicates
			alreadyFoundUsers.add(dto.getUserId().toLowerCase());
		}

		return result;
	}
	
	public List<PersonWithActiveSOFDAffiliationsReportDTO> generatePersonsWithActiveSOFDAffiliationsReport() {
		List<PersonWithActiveSOFDAffiliationsReportDTO> activeAffiliationsReportDTO = new ArrayList<>();

		for (Person person : personService.getActiveCached()) {
			User primeUser = person.getUsers().stream().map(um -> um.getUser())
					.filter(u -> u.isPrime() && SupportedUserTypeService.isActiveDirectory(u.getUserType()))
					.findAny()
					.orElse(null);

			// filter affiliations
			List<Affiliation> affiliations = AffiliationService.notStoppedAffiliations(person.getAffiliations())
					.stream()
					.filter(a -> a.getMaster().equals("SOFD"))
					.collect(Collectors.toList());
			
			// at least one not-stopped SOFD affiliation
			if (affiliations.size() >= 1) {
				for (Affiliation affiliation : affiliations) {
					PersonWithActiveSOFDAffiliationsReportDTO dto = new PersonWithActiveSOFDAffiliationsReportDTO();
					dto.setUuid(person.getUuid());
					dto.setName(PersonService.getName(person));
					dto.setUserId(primeUser != null ? primeUser.getUserId() : null);
					dto.setAffiliationName(AffiliationService.getPositionName(affiliation));
					dto.setAffiliationOrgUnitName(affiliation.getCalculatedOrgUnit().getName());

					activeAffiliationsReportDTO.add(dto);
				}
			}
		}

		return activeAffiliationsReportDTO;
	}

	public List<ActiveAffiliationOrActiveADAccountReportDTO> generateActiveAffiliationOrActiveADAccountReport() {
		List<ActiveAffiliationOrActiveADAccountReportDTO> result = new ArrayList<>();

		for (Person person : personService.getActiveCached()) {
			var activeADUsers = person.getUsers().stream().map(um -> um.getUser())
					.filter(u -> !u.isDisabled() && SupportedUserTypeService.isActiveDirectory(u.getUserType())).toList();

			var activeAffiliations = AffiliationService.notStoppedAffiliations(person.getAffiliations()).stream().toList();

			if (!activeAffiliations.isEmpty() || !activeADUsers.isEmpty()) {
				var resultRow = new ActiveAffiliationOrActiveADAccountReportDTO();
				resultRow.setUuid(person.getUuid());
				resultRow.setName(PersonService.getName(person));
				resultRow.setUsers(activeADUsers.stream().map(User::getUserId).toList());
				resultRow.setAffiliations(activeAffiliations.stream().map(a -> AffiliationService.getPositionName(a) + " i " + a.getOrgUnit().getName()).toList());
				result.add(resultRow);
			}
		}
		return result;
	}
	
	public List<PersonWithAffiliationsWorkplacesReportDTO> generatePersonWithAffiliationsWorkplacesReport() {
		List<PersonWithAffiliationsWorkplacesReportDTO> reportDTOs = new ArrayList<>();
		
		for (Person person : personService.getAll()) {
			for (Affiliation affiliation : AffiliationService.notStoppedAffiliations(person.getAffiliations())) {
				if( affiliation.getWorkplaces() == null || affiliation.getWorkplaces().size() == 0) {
					PersonWithAffiliationsWorkplacesReportDTO dto = new PersonWithAffiliationsWorkplacesReportDTO();
					dto.setUuid(person.getUuid());
					dto.setName(PersonService.getName(person));
					dto.setAffiliationUuid(affiliation.getUuid());
					var employeeNumber = Objects.equals(configuration.getModules().getLos().getPrimeAffiliationMaster(), affiliation.getMaster()) ? affiliation.getMasterId() : "";
					dto.setEmployeeNumber(employeeNumber);
					dto.setPositionName(AffiliationService.getPositionName(affiliation));
					dto.setAffiliationStartDate(affiliation.getStartDate() != null ? affiliation.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString() : "");
					dto.setAffiliationStopDate(affiliation.getStopDate() != null ? affiliation.getStopDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString() : "");
					dto.setAffiliationPlacement(placementString(affiliation.getOrgUnit(), affiliation.getOrgUnit().getName()));
					dto.setWorkplace("Ingen arbejdssteder angivet");
					dto.setWorkplaceStartDate("");
					dto.setWorkplaceStopDate("");
					reportDTOs.add(dto);
				}
				else {
					for (Workplace workplace : affiliation.getWorkplaces()) {
						PersonWithAffiliationsWorkplacesReportDTO dto = new PersonWithAffiliationsWorkplacesReportDTO();
						dto.setUuid(person.getUuid());
						dto.setName(PersonService.getName(person));
						dto.setAffiliationUuid(affiliation.getUuid());
						var employeeNumber = Objects.equals(configuration.getModules().getLos().getPrimeAffiliationMaster(), affiliation.getMaster()) ? affiliation.getMasterId() : "";
						dto.setEmployeeNumber(employeeNumber);
						dto.setPositionName(AffiliationService.getPositionName(affiliation));
						dto.setAffiliationStartDate(affiliation.getStartDate() != null ? affiliation.getStartDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString() : "");
						dto.setAffiliationStopDate(affiliation.getStopDate() != null ? affiliation.getStopDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate().toString() : "");
						dto.setAffiliationPlacement(placementString(affiliation.getOrgUnit(), affiliation.getOrgUnit().getName()));
						dto.setWorkplace(workplace.getOrgUnit().getName());
						dto.setWorkplaceStartDate(workplace.getStartDate() != null ? workplace.getStartDate().toString() : "");
						dto.setWorkplaceStopDate(workplace.getStopDate() != null ? workplace.getStopDate().toString() : "");
						reportDTOs.add(dto);
					}
				}
			}
		}
		return reportDTOs;
	}
	
	public String placementString(OrgUnit orgunit, String placement) {
		if (orgunit.getParent() != null) {
			return placementString(orgunit.getParent(), orgunit.getParent().getName() + "/" + placement);
		}
		return placement;
	}

}
