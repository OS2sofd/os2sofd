package dk.digitalidentity.sofd.dao;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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
			"with recursive cte as" +
			"(" +
			"    select" +
			"        o.uuid" +
			"        ,o.parent_uuid" +
			"        ,e.number as ean" +
			"        ,e.id" +
			"    from" +
			"        orgunits o" +
			"    left join ean e on e.orgunit_uuid = o.uuid" +
			"    where" +
			"        o.parent_uuid is null" +
			"        and o.deleted = 0" +
			"    union all" +
			"    select" +
			"        o.uuid" +
			"        ,o.parent_uuid" +
			"        ,ifnull(e.number, parent.ean) as ean" +
			"        ,ifnull(e.id, parent.id)" +
			"    from" +
			"        orgunits o" +
			"    left join ean e on e.orgunit_uuid = o.uuid" +
			"    inner join cte parent on parent.uuid = o.parent_uuid" +
			"    where" +
			"        o.deleted = 0" +
			")" +
			"select distinct id from cte where uuid = :uuid")
	List<Long> getInheritedEan(@Param("uuid") String uuid);

	// returns uuids of all orgunits that are marked not to be transferred to FK Org - including children
	@Query(nativeQuery = true, value = """
		with recursive orgcte as
		(
			select
				o.uuid,
				o.parent_uuid,
				o.do_not_transfer_to_fk_org
			from orgunits o
			inner join organisations adm on adm.short_name = 'ADMORG' and adm.id = o.belongs_to
			where
				o.deleted = 0
				and o.parent_uuid is NULL
			union all
			select
				o.uuid,
				o.parent_uuid,
				if(p.do_not_transfer_to_fk_org or o.do_not_transfer_to_fk_org,true,false) as do_not_transfer_to_fk_org
			from orgunits o
			inner join orgcte p on p.uuid = o.parent_uuid
			where
				o.deleted = 0
		)
		select
			uuid
		from orgcte
		where
			do_not_transfer_to_fk_org = true;
		""")
	Set<String> getDoNotTransferToFKOrgUuids();

	// todo: only used once per municipality from OrgUnitManagerMigrationOneOff - remove once all are migrated
	@Modifying
	@Query(nativeQuery = true, value = """
				update orgunits o
				left join orgunits_manager om on om.orgunit_uuid = o.uuid and om.inherited = 0
				set o.selected_manager_uuid = om.manager_uuid
			""")
	void migrateSelectedManagers();

	// todo: only used once per municipality from OrgUnitManagerMigrationOneOff - remove once all are migrated
	@Modifying
	@Query(nativeQuery = true, value = """
				update orgunits o
				left join orgunits_manager om on om.orgunit_uuid = o.uuid and om.inherited = 0
				set o.imported_manager_uuid = om.manager_uuid
			""")
	void migrateImportedManagers();

	@Query(nativeQuery = true, value = """
				select * from orgunits o
				where
					o.name like concat(:query,'%')
					or o.shortname like concat(:query,'%')
					or o.display_name like concat(:query,'%')
					or o.source_name like concat(:query,'%')
					or o.pnr like concat(:query,'%')
					or o.cvr like concat(:query,'%')
				order by ifnull(o.display_name,o.name)
				limit 10
			""")
	List<OrgUnit> searchOrgUnits(@Param("query") String query);
}
