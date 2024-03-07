package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.OrgUnitType;
import dk.digitalidentity.sofd.dao.model.Organisation;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.security.RequireDaoWriteAccess;

public interface OrgUnitDao extends JpaRepository<OrgUnit, String> {

	List<OrgUnit> findByManagerManager(Person manager);

	List<OrgUnit> findByDeletedFalseAndBelongsTo(Organisation organisation);

	List<OrgUnit> findByBelongsTo(Organisation organisation);

	Page<OrgUnit> findByBelongsTo(Organisation organisation, Pageable pageable);

	OrgUnit findByMasterId(String masterId);

	@Query(nativeQuery = true, value =
			"SELECT o.* FROM orgunits o " +
			" INNER JOIN affiliations a ON a.orgunit_uuid = o.uuid " +
			" WHERE o.deleted = 0 AND belongs_to = ?1 " +
			" GROUP BY o.uuid")
	List<OrgUnit> findAllActiveWithAffiliations(long admOrgId);

	List<OrgUnit> findByUuidIn(List<String> uuids);

	OrgUnit findByUuid(String uuid);

	List<OrgUnit> findBySourceName(String sourceName);

	@RequireDaoWriteAccess
	<S extends OrgUnit> List<S> save(Iterable<S> entities);

	@RequireDaoWriteAccess
	<S extends OrgUnit> S save(S entity);

	@RequireDaoWriteAccess
	<S extends OrgUnit> S saveAndFlush(S entity);

	List<OrgUnit> findByType(OrgUnitType type);
	List<OrgUnit> findByOrgTypeId(Long orgTypeId);

	@Query(nativeQuery = true, value =
			"with recursive cte as " +
			"( " +
			"    select " +
			"        o.uuid " +
			"        ,o.parent_uuid " +
			"        ,o.ean " +
			"    from " +
			"        orgunits o " +
			"    where " +
			"        o.parent_uuid is null " +
			"        and o.deleted = 0 " +
			"    union all " +
			"    select " +
			"        o.uuid " +
			"        ,o.parent_uuid " +
			"        ,ifnull(o.ean, parent.ean) as ean " +
			"    from " +
			"        orgunits o " +
			"    inner join cte parent on parent.uuid = o.parent_uuid " +
			"    where " +
			"        o.deleted = 0 " +
			") " +
			"select ean from cte where uuid = :uuid")
    String getInheritedEan(@Param("uuid") String uuid);

}
