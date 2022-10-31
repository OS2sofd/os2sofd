package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.Affiliation;

public interface AffiliationDao extends CrudRepository<Affiliation, Long> {
	
	List<Affiliation> findAll();

	Affiliation findByUuid(String uuid);
	
	List<Affiliation> findByMaster(String master);

	@Query(value = "SELECT COUNT(*) FROM affiliations WHERE orgunit_uuid = ?1 AND deleted = 0 AND (stop_date IS NULL OR CAST(stop_date AS DATE) >= CAST(CURRENT_TIMESTAMP AS DATE))", nativeQuery = true)
	Long countByOrgUnitAndActive(String orgUnitUuid);
	
	// TODO: should this respect force_stop_date?
	@Query(value = "SELECT * FROM affiliations a WHERE orgunit_uuid = ?1 AND deleted = 0 AND (stop_date IS NULL OR CAST(stop_date AS DATE) >= CAST(CURRENT_TIMESTAMP AS DATE))", nativeQuery = true)
	List<Affiliation> findByOrgUnitAndActive(String orgUnitUuid);
}