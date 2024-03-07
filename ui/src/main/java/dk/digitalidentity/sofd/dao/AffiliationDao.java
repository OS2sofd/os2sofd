package dk.digitalidentity.sofd.dao;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.OrgUnit;

public interface AffiliationDao extends CrudRepository<Affiliation, Long> {
	
	List<Affiliation> findAll();

	Affiliation findByUuid(String uuid);
	
	List<Affiliation> findByMaster(String master);

	List<Affiliation> findByOrgUnit(OrgUnit orgUnit);

	@Query(value = "SELECT * FROM affiliations a WHERE (alt_orgunit_uuid IS NOT NULL AND alt_orgunit_uuid = ?1 ) OR (alt_orgunit_uuid IS NULL AND orgunit_uuid = ?1 )", nativeQuery = true)
	List<Affiliation> findByCalculatedOrgUnit(String orgUnitUuid);

	@Query(value = "SELECT COUNT(*) FROM affiliations WHERE orgunit_uuid = ?1 AND deleted = 0 AND (stop_date IS NULL OR CAST(stop_date AS DATE) >= CAST(CURRENT_TIMESTAMP AS DATE))", nativeQuery = true)
	Long countByOrgUnitAndActive(String orgUnitUuid);
	
	// TODO: should this respect force_stop_date?
	@Query(value = "SELECT * FROM affiliations a WHERE orgunit_uuid = ?1 AND deleted = 0 AND (stop_date IS NULL OR CAST(stop_date AS DATE) >= CAST(CURRENT_TIMESTAMP AS DATE))", nativeQuery = true)
	List<Affiliation> findByOrgUnitAndActive(String orgUnitUuid);

	@Query(value = "SELECT * FROM affiliations a WHERE ((alt_orgunit_uuid IS NOT NULL AND alt_orgunit_uuid = ?1 ) OR (alt_orgunit_uuid IS NULL AND orgunit_uuid = ?1 )) AND deleted = 0 AND (stop_date IS NULL OR CAST(stop_date AS DATE) >= CAST(CURRENT_TIMESTAMP AS DATE))", nativeQuery = true)
	List<Affiliation> findByCalculatedOrgUnitAndActive(String orgUnitUuid);
	@Modifying
	@Query(value = "DELETE FROM affiliations WHERE date(start_date) = date(stop_date) AND master = 'OPUS'", nativeQuery = true)
	void deleteInvalidOpusAffiliations();

	@Modifying
	void deleteByMasterAndStopDateNotNullAndStopDateBefore(String master, Date date);

	List<Affiliation> findByEmployeeId(String employeeId);
	List<Affiliation> findByUseAsPrimaryWhenActiveTrue();
}