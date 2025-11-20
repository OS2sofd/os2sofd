package dk.digitalidentity.sofd.dao;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.query.Param;

import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.RevisionId;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.security.RequireDaoWriteAccess;
import dk.digitalidentity.sofd.security.RequirePersonCreaterOrDaoWriteAccess;

public interface PersonDao extends JpaRepository<Person, String>, JpaSpecificationExecutor<Person>, RevisionRepository<Person, String, Integer> {

	<S extends Person>S findByCpr(String cpr);
	
	@RequireDaoWriteAccess
	<S extends Person> List<S> save(Iterable<S> entities);

	@RequirePersonCreaterOrDaoWriteAccess
	<S extends Person> S save(S entity);

	@RequireDaoWriteAccess
	<S extends Person> S saveAndFlush(S entity);
	
	@Query(nativeQuery = true, value = "SELECT MAX(last_changed) FROM persons")
	Date getLatestUpdate();

	<S extends Person> List<S> findDistinctByUsersUserUserTypeAndDeletedFalse(String userType);
	
	<S extends Person> List<S> findByUsersUserUserTypeAndUsersUserUserId(String userType, String userId);

	<S extends Person> List<S> findByUsersUserUserTypeAndUsersUserMasterId(String userType, String masterId);
	Person findByUsersUserActiveDirectoryDetailsKombitUuid(String kombitUuid);

	<S extends Person>S findByUsersUser(User user);

	<S extends Person> List<S> findByDeletedTrue();

	<S extends Person> List<S> findByTaxedPhoneTrue();
	
	<S extends Person> List<S> findByLeaveNotNull();

	@Query(nativeQuery = true, value= """
				select p.*
				from persons p
				inner join affiliations a on
					a.person_uuid = p.`uuid`
					and a.master = :master
					and date(a.stop_date) = date(:stopDate)
				group by p.uuid
			""")
	List<Person> findByAffiliationsMasterAndAffiliationsStopDate(String master, Date stopDate);

	// Search prefixes
	@Query(nativeQuery = true, value = """
				select p.*
				from persons p
				left join persons_users pu on pu.person_uuid = p.uuid
				left join users u on u.id = pu.user_id and u.user_type in ('ACTIVE_DIRECTORY','ACTIVE_DIRECTORY_SCHOOL','SCHOOL_EMAIL','EXCHANGE')
				left join active_directory_details ad on ad.user_id = u.id
				left join affiliations a on a.person_uuid = p.uuid
				where
					p.uuid = :query
					or ad.kombit_uuid = :query
					or p.firstname like concat(:query,'%')
					or p.surname like concat(:query,'%')
					or p.chosen_name like concat(:query,'%')
					or concat (p.firstname,' ',p.surname) like concat(:query,'%')
					or u.user_id like concat(:query,'%')
					or trim(leading '0' from a.employee_id) = trim(leading '0' from :query)
					or (:cprAccess and p.cpr like concat(:query,'%'))
					or p.key_words like concat('%',:query,'%')
					or a.vendor like concat('%',:query,'%')
				group by p.uuid
				order by ifnull(p.chosen_name,concat(p.firstname,p.surname))
				limit 20
			""")
	List<Person> searchPersons(String query, boolean cprAccess);

	List<Person> findTop10ByCprStartingWith(String prefix);

	Person findByUuid(String uuid);
	
	@Modifying
	@Query(nativeQuery = true, value = "DELETE FROM persons WHERE uuid IN ?1")
	void deleteByUuid(Set<String> uuids);
	
	@Query(nativeQuery = true, value = "SELECT uuid FROM persons WHERE disable_account_orders_create = 1")
	List<String> getUuidsOfDisableAccountOrderPersons();

	@Query(nativeQuery = true, value = "SELECT p.* " +
			"FROM persons p" +
			" WHERE p.deleted = 0" +
			"  AND (p.firstname LIKE CONCAT('%', ?1, '%')" +
			"   OR  p.surname LIKE CONCAT('%', ?1, '%')" +
			"   OR  p.chosen_name LIKE CONCAT('%',?1,'%')" +
			"   OR CONCAT(p.firstname,' ', p.surname) LIKE CONCAT('%',?1,'%'))" +
			" ORDER BY p.firstname, p.surname LIMIT 10")
	List<Person> findTop10ByName(@Param("name") String input);

	@Query(nativeQuery = true, value = """
			select p.*
			from persons p
			inner join affiliations a on a.person_uuid = p.`uuid` and a.prime
			left join persons_leave pl on pl.id = p.leave_id and now() between ifnull(pl.start_date,now()) and ifnull(pl.stop_date,now())
			where
				p.deleted = 0
				and p.force_stop = 0
				and p.dead = 0
				and pl.id is null		
				and (
					p.firstname like concat('%',?1,'%')
					or p.surname like concat('%',?1,'%')
					or p.chosen_name like concat('%',?1,'%')
					or concat(p.firstname,' ',p.surname) like concat('%',?1,'%')
				)
			order by ifnull(p.chosen_name, concat(p.firstname,p.surname))
			limit 10			
			""")
	List<Person> findTop10ValidManagersByName(@Param("name") String name);

	@Query(nativeQuery = true, value = "SELECT p.* " + 
			"FROM   persons p " + 
			"       JOIN persons_users pu ON p.uuid = pu.person_uuid " +
			"       JOIN users u ON u.id = pu.user_id " +
			"       JOIN active_directory_details ad ON ad.user_id = u.id " +
			"WHERE  u.user_type = 'ACTIVE_DIRECTORY' " + 
			"       AND ad.password_expire_date = ?1")
	List<Person> getPersonsWithADPwdChangePending(String date);
	
	// TODO: should these also respect force_stop_date ?
	
	@Query(nativeQuery = true, value = "SELECT DISTINCT p.* " + 
			"FROM   persons p " + 
			"       INNER JOIN orgunits_manager om " +
			"               ON om.manager_uuid = p.uuid and om.inherited = 0")
	<S extends Person> List<S> getManagers();

	@Query(nativeQuery = true, value = "SELECT p.* " +
			"FROM   persons p " + 
			"       INNER JOIN affiliations a " + 
			"               ON a.person_uuid = p.uuid " + 
			"       INNER JOIN affiliations_function af " + 
			"               ON af.affiliation_id = a.id " + 
			"WHERE  a.deleted = 0 " + 
			"       AND (a.start_date IS NULL OR CAST(a.start_date AS DATE) <= CAST(CURRENT_TIMESTAMP AS DATE))" +
			"       AND (a.stop_date IS NULL OR CAST(a.stop_date AS DATE) >= CAST(CURRENT_TIMESTAMP AS DATE))" +
			"       AND (af.function = 'TR' OR af.function = 'TR_SUPPLEANT')")
	List<Person> getTRs();

	@Query(nativeQuery = true, value = "SELECT p.* " + 
			"FROM   persons p " + 
			"       INNER JOIN affiliations a " + 
			"               ON a.person_uuid = p.uuid " + 
			"       INNER JOIN affiliations_function af " + 
			"               ON af.affiliation_id = a.id " + 
			"WHERE  a.deleted = 0 " + 
			"       AND (a.start_date IS NULL OR CAST(a.start_date AS DATE) <= CAST(CURRENT_TIMESTAMP AS DATE))" +
			"       AND (a.stop_date IS NULL OR CAST(a.stop_date AS DATE) >= CAST(CURRENT_TIMESTAMP AS DATE))" +
			"       AND (af.function = 'SR')")
	List<Person> getSRs();
	
	@Query(nativeQuery = true, value = """
			select count(*)
			from
			substitute_context c
			left join substitute_assignment sa on sa.substitute_context_id = c.id and sa.substitute_uuid = ?1
			left join substitute_org_unit_assignment soa on soa.substitute_context_id = c.id and soa.substitute_uuid  = ?1
			where
				c.identifier in ('SOFD','GLOBAL')
				and (sa.id is not null or soa.id is not null)
		""")
	long countSofdSubstituteAssignments(String personUuid);

	<S extends Person> List<S> findByDeletedFalse();

	@Query(nativeQuery = true, value = """
    SELECT pa.rev AS rev, 
           r.auditor_name as auditorName,
           CONVERT_TZ(TIMESTAMPADD(SECOND, r.timestamp/1000, '1970-01-01 00:00:00'), '+00:00', '+01:00') as lastChanged 
    FROM persons_aud pa 
    JOIN revisions r ON r.id = pa.rev 
    WHERE pa.uuid = ?1 
    ORDER BY r.id
    """)
	List<RevisionId> getRevisionIds(String id);

	@Modifying
	@Query(nativeQuery = true, value = "DELETE FROM persons_aud WHERE uuid = ?1")
	void deletePersonLog(String uuid);

	List<Person> findByForceStopTrueOrDisableAccountOrdersCreateTrueOrLeaveNotNull();

	List<Person> findByPhonesPhoneMasterAndPhonesPhoneMasterId(String master, String masterId);

	@Query(nativeQuery = true, value= """
		select p.*
		from persons p
		left join affiliations a on a.person_uuid = p.`uuid` and a.prime
		group by p.`uuid`
		order by count(a.id) desc, rand()
		limit :limit
	""")
	List<Person> getRandomPeople(@Param("limit") int limit);

	@Query(nativeQuery = true, value = "SELECT * FROM persons WHERE uuid > :offset ORDER BY uuid LIMIT :size")
	List<Person> findLimitedWithOffset(int size, String offset);

	@Query(nativeQuery = true, value = "SELECT * FROM persons ORDER BY uuid LIMIT :size")
	List<Person> findLimited(int size);

	Person findByAffiliationsId(long affiliationId);

	Person findByAffiliationsUuid(String affiliationsUuid);

	Person findByAffiliationsMasterId(String masterId);

	Person findByAffiliationsMasterAndAffiliationsMasterId(String master, String masterId);

	@EntityGraph(attributePaths = {"affiliations"})
    List<Person> findByChosenNameNotNull();
}