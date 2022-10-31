package dk.digitalidentity.sofd.dao;

import java.util.Date;
import java.util.List;
import java.util.Set;

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

	void delete(Person entity);

	void deleteById(String id);

	void deleteAll();

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

	<S extends Person>S findByUsersUser(User user);

	<S extends Person> List<S> findByDeletedTrue();

	<S extends Person> List<S> findByTaxedPhoneTrue();
	
	<S extends Person> List<S> findByLeaveNotNull();

	// TODO: doubt this works.... what happens if the time part is not 00:00:00 ? what about timezones?
	List<Person> findByAffiliationsMasterAndAffiliationsStopDate(String master, Date date);

	Person findByUuid(String uuid);
	
	@Modifying
	@Query(nativeQuery = true, value = "DELETE FROM persons WHERE uuid IN ?1")
	void deleteByUuid(Set<String> uuids);
	
	@Query(nativeQuery = true, value = "SELECT uuid FROM persons WHERE disable_account_orders = 1")
	List<String> getUuidsOfDisableAccountOrderPersons();

	@Query(nativeQuery = true, value = "SELECT p.* " +
			"FROM persons p" +
			" WHERE p.deleted = 0" +
			"  AND (p.firstname LIKE CONCAT('%', ?1, '%')" +
			"   OR  p.surname LIKE CONCAT('%', ?1, '%')" +
			"   OR  p.chosen_name LIKE CONCAT('%',?1,'%'))" +
			" ORDER BY p.firstname LIMIT 10")
	List<Person> findTop10ByName(@Param("name") String input);

	@Query(nativeQuery = true, value = "SELECT p.* " + 
			"FROM   persons p " + 
			"       JOIN persons_users pu ON p.uuid = pu.person_uuid " +
			"       JOIN users u ON u.id = pu.user_id " +
			"       JOIN active_directory_details ad ON ad.user_id = u.id " +
			"WHERE  u.user_type = 'ACTIVE_DIRECTORY' " + 
			"       AND ad.password_expire_date = ?1")
	List<Person> getPersonsWithADPwdChangePending(String date);
	
	// TODO: should these also respect force_stop_date ?
	
	@Query(nativeQuery = true, value = "SELECT p.* " + 
			"FROM   persons p " + 
			"       INNER JOIN affiliations a " + 
			"               ON a.person_uuid = p.uuid " + 
			"       INNER JOIN affiliations_manager am " + 
			"               ON am.affiliation_id = a.id " + 
			"WHERE  a.deleted = 0 " + 
			"       AND (a.start_date IS NULL OR CAST(a.start_date AS DATE) <= CAST(CURRENT_TIMESTAMP AS DATE))" +
			"       AND (a.stop_date IS NULL OR CAST(a.stop_date AS DATE) >= CAST(CURRENT_TIMESTAMP AS DATE))")
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

	<S extends Person> List<S> findByDeletedFalse();
	
	@Query(nativeQuery = true, value = "SELECT rev, last_changed AS lastChanged FROM persons_aud WHERE uuid = ?1")
	List<RevisionId> getRevisionIds(String id);

	@Modifying
	@Query(nativeQuery = true, value = "DELETE FROM persons_aud WHERE uuid = ?1")
	void deletePersonLog(String uuid);

	List<Person> findByForceStopTrueOrDisableAccountOrdersTrueOrLeaveNotNull();
}
