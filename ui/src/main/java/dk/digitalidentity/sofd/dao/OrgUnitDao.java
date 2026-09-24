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
import org.springframework.transaction.annotation.Transactional;

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

	List<OrgUnit> findBySourceNameAndDeleted(String sourceName, boolean deleted);

	List<OrgUnit> findByPhonesPhoneMasterAndPhonesPhoneMasterId(String master, String masterId);

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

	interface EffectiveAffiliationDurationRule {
		String getUuid();
		String getName();
		String getRule();
		Integer getMaxDays();
	}

	// walks up from the given orgunit and returns the nearest ancestor (or the orgunit itself) with a rule other than INHERIT
	@Query(nativeQuery = true, value = """
		with recursive cte as
		(
			select
				o.uuid,
				o.parent_uuid,
				o.name,
				o.affiliation_duration_rule,
				o.affiliation_max_days,
				0 as depth
			from orgunits o
			where
				o.uuid = :uuid
			union all
			select
				p.uuid,
				p.parent_uuid,
				p.name,
				p.affiliation_duration_rule,
				p.affiliation_max_days,
				c.depth + 1
			from orgunits p
			inner join cte c on c.parent_uuid = p.uuid
		)
		select
			uuid as uuid,
			name as name,
			affiliation_duration_rule as rule,
			affiliation_max_days as maxDays
		from cte
		where
			affiliation_duration_rule <> 'INHERIT'
		order by depth
		limit 1
		""")
	EffectiveAffiliationDurationRule getEffectiveAffiliationDurationRule(@Param("uuid") String uuid);

	@Query(nativeQuery = true, value = """
				select o.* from orgunits o
				left outer join ean on ean.orgunit_uuid = o.`uuid`
				where
					o.deleted = 0
					and (
						o.uuid like concat(:query,'%')
						or o.master_id like concat(:query,'%')
						or o.name like concat('%',:query,'%')
						or o.shortname like concat('%',:query,'%')
						or o.display_name like concat('%',:query,'%')
						or o.source_name like concat('%',:query,'%')
						or o.pnr like concat(:query,'%')
						or o.cvr like concat(:query,'%')
						or ean.`number` like concat(:query,'%')
					)
				group by o.uuid
				order by ifnull(o.display_name,o.name)
				limit 20
			""")
	List<OrgUnit> searchOrgUnits(@Param("query") String query);

	@Query(nativeQuery = true, value = """
		with recursive cte as
		(
			select
				o.uuid,
				o.parent_uuid,
				o.deleted,
				0 as ancestor_deleted
			from orgunits o
			where o.parent_uuid is null

			union all

			select
				o.uuid,
				o.parent_uuid,
				o.deleted,
				cte.deleted or cte.ancestor_deleted as ancestor_deleted
			from orgunits o
			inner join cte on cte.uuid = o.parent_uuid
		)
		select o.* from cte
		inner join orgunits o on o.`uuid` = cte.uuid
		where
			cte.ancestor_deleted = 1
			and o.deleted = 0
		""")
    List<OrgUnit> getActiveWhereAncestorDeleted();

	@Query(nativeQuery = true, value = "SELECT * FROM orgunits WHERE uuid > :offset ORDER BY uuid LIMIT :size")
	List<OrgUnit> findLimitedWithOffset(int size, String offset);

	@Query(nativeQuery = true, value = "SELECT * FROM orgunits ORDER BY uuid LIMIT :size")
	List<OrgUnit> findLimited(int size);

	@RequireDaoWriteAccess
	@Modifying
	@Transactional
	@Query(nativeQuery = true, value = "CALL update_orgunit_manager_recursive(null, true)")
	void updateOrgUnitManagers();
}
