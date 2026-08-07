package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import dk.digitalidentity.sofd.dao.model.Profession;
import dk.digitalidentity.sofd.dao.model.projection.ProfessionFieldCount;
import dk.digitalidentity.sofd.dao.model.projection.ProfessionLookup;

public interface ProfessionDao extends CrudRepository<Profession, Long> {
	List<Profession> findByOrganisationId(long organisationId);

	@Query(nativeQuery = true, value = """
			select
			  distinct a.position_name
			from organisations org
			inner join orgunits o on o.belongs_to = org.id and o.deleted = 0
			inner join affiliations a on a.orgunit_uuid = o.uuid and a.deleted = 0
			where
			  org.id = :organisationId
			order by a.position_name
			""")
	List<String> getUniquePositionNames(@Param("organisationId") long organisationId);

	@Query(nativeQuery = true, value = """
			select
			  distinct a.position_type_name
			from organisations org
			inner join orgunits o on o.belongs_to = org.id and o.deleted = 0
			inner join affiliations a on a.orgunit_uuid = o.uuid and a.deleted = 0
			where
			  org.id = :organisationId
			order by a.position_type_name
			""")
	List<String> getUniquePositionTypeNames(@Param("organisationId") long organisationId);
	
	@Query(nativeQuery = true, value = """
			select
			  distinct a.pay_grade
			from organisations org
			inner join orgunits o on o.belongs_to = org.id and o.deleted = 0
			inner join affiliations a on a.orgunit_uuid = o.uuid and a.deleted = 0
			where
			  org.id = :organisationId
			order by a.pay_grade
			""")
	List<String> getUniquePayGrades(@Param("organisationId") long organisationId);

	@Query(nativeQuery = true, value = """
			select
			  a.position_name as fieldValue,
			  count(*) as activeCount
			from organisations org
			inner join orgunits o on o.belongs_to = org.id and o.deleted = 0
			inner join affiliations a on a.orgunit_uuid = o.uuid and a.deleted = 0
			where
			  org.id = :organisationId
			  and (a.start_date is null or a.start_date <= curdate())
			  and (a.stop_date is null or a.stop_date >= curdate())
			group by a.position_name
			""")
	List<ProfessionFieldCount> getActivePositionNameCounts(@Param("organisationId") long organisationId);

	@Query(nativeQuery = true, value = """
			select
			  a.position_type_name as fieldValue,
			  count(*) as activeCount
			from organisations org
			inner join orgunits o on o.belongs_to = org.id and o.deleted = 0
			inner join affiliations a on a.orgunit_uuid = o.uuid and a.deleted = 0
			where
			  org.id = :organisationId
			  and (a.start_date is null or a.start_date <= curdate())
			  and (a.stop_date is null or a.stop_date >= curdate())
			group by a.position_type_name
			""")
	List<ProfessionFieldCount> getActivePositionTypeNameCounts(@Param("organisationId") long organisationId);

	@Query(nativeQuery = true, value = """
			select
			  a.pay_grade as fieldValue,
			  count(*) as activeCount
			from organisations org
			inner join orgunits o on o.belongs_to = org.id and o.deleted = 0
			inner join affiliations a on a.orgunit_uuid = o.uuid and a.deleted = 0
			where
			  org.id = :organisationId
			  and (a.start_date is null or a.start_date <= curdate())
			  and (a.stop_date is null or a.stop_date >= curdate())
			group by a.pay_grade
			""")
	List<ProfessionFieldCount> getActivePayGradeCounts(@Param("organisationId") long organisationId);

	@Query(nativeQuery = true, value= """
			select
				a.id as affiliationId,
				a.position_name as positionName,
				a.pay_grade as payGrade,
				a.position_type_name as positionTypeName,
				a.profession_id as professionId,
				o.belongs_to as organisationId
			from affiliations a
			inner join orgunits o on o.uuid = a.orgunit_uuid
			""")
	List<ProfessionLookup> getProfessionLookup();

	List<Profession> findAll();

    boolean existsByOrganisationIdAndName(Long organisationId, String name);
}