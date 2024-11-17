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

	@Query(value = "SELECT * FROM affiliations a LEFT JOIN (SELECT orgunit_uuid, affiliation_id FROM affiliations_workplaces aw WHERE aw.start_date <= curdate() AND aw.stop_date >= curdate()) workplace ON workplace.affiliation_id = a.id WHERE COALESCE(workplace.orgunit_uuid, a.`alt_orgunit_uuid`, a.`orgunit_uuid`)  = ?1", nativeQuery = true)
	List<Affiliation> findByCalculatedOrgUnit(String orgUnitUuid);

	@Query(value = "SELECT COUNT(*) FROM affiliations a LEFT JOIN (SELECT orgunit_uuid, affiliation_id FROM affiliations_workplaces aw WHERE aw.start_date <= curdate() AND aw.stop_date >= curdate()) workplace ON workplace.affiliation_id = a.id WHERE COALESCE(workplace.orgunit_uuid, a.`alt_orgunit_uuid`, a.`orgunit_uuid`) = ?1 AND deleted = 0 AND (stop_date IS NULL OR CAST(stop_date AS DATE) >= CAST(CURRENT_TIMESTAMP AS DATE))", nativeQuery = true)
	Long countByOrgUnitAndActive(String orgUnitUuid);

	@Query(value = """
			WITH RECURSIVE cte AS (
				SELECT o.uuid, o.parent_uuid FROM orgunits o WHERE uuid = ?1
				UNION ALL
				SELECT o.uuid, o.parent_uuid FROM orgunits o INNER JOIN cte on cte.uuid = o.parent_uuid
			)
			SELECT COUNT(*)
			FROM affiliations a
			LEFT JOIN affiliations_workplaces aw ON
				aw.affiliation_id = a.id
				AND aw.start_date <= curdate()
				AND aw.stop_date >= curdate()
			WHERE
				a.deleted = 0
				AND (a.stop_date IS NULL OR a.stop_date >= curdate())
				AND COALESCE(aw.orgunit_uuid, a.alt_orgunit_uuid, a.orgunit_uuid) IN (SELECT uuid FROM cte)
			""", nativeQuery = true)
	Long countByOrgUnitAndActiveRecursive(String orgUnitUuid);

	@Query(value = "SELECT * FROM affiliations a LEFT JOIN (SELECT orgunit_uuid, affiliation_id FROM affiliations_workplaces aw WHERE aw.start_date <= curdate() AND aw.stop_date >= curdate()) workplace ON workplace.affiliation_id = a.id WHERE COALESCE(workplace.orgunit_uuid, a.alt_orgunit_uuid, a.orgunit_uuid) = ?1 AND deleted = 0 AND (stop_date IS NULL OR CAST(stop_date AS DATE) >= CAST(CURRENT_TIMESTAMP AS DATE))", nativeQuery = true)
	List<Affiliation> findByCalculatedOrgUnitAndActive(String orgUnitUuid);

	@Modifying
	@Query(value = "DELETE FROM affiliations WHERE date(start_date) = date(stop_date) AND master = 'OPUS'", nativeQuery = true)
	void deleteInvalidOpusAffiliations();

	@Modifying
	void deleteByMasterAndStopDateNotNullAndStopDateBefore(String master, Date date);

	List<Affiliation> findByEmployeeId(String employeeId);
	List<Affiliation> findByUseAsPrimaryWhenActiveTrue();

	@Query(nativeQuery = true, value = """
			select
				case when count(*) > 0 then 'true' else 'false' end
			from
			(
				select person_uuid,master from affiliations
				union
				select person_uuid,master from affiliations_aud
			) sub
			where
				sub.person_uuid = :personUuid
				and sub.master in (:masters)
			""")
	boolean anyAffiliationWithMasterInExisted(String personUuid, List<String> masters);
}