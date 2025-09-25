package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import dk.digitalidentity.sofd.dao.model.Profession;
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
			  distinct a.pay_grade
			from organisations org
			inner join orgunits o on o.belongs_to = org.id and o.deleted = 0
			inner join affiliations a on a.orgunit_uuid = o.uuid and a.deleted = 0
			where
			  org.id = :organisationId
			order by a.pay_grade
			""")
	List<String> getUniquePayGrades(@Param("organisationId") long organisationId);

	@Query(nativeQuery = true, value= """
			select
				a.id as affiliationId,
				a.position_name as positionName,
				a.pay_grade as payGrade,
				a.profession_id as professionId,
				o.belongs_to as organisationId
			from affiliations a
			inner join orgunits o on o.uuid = a.orgunit_uuid
			""")
	List<ProfessionLookup> getProfessionLookup();

	List<Profession> findAll();

    boolean existsByOrganisationIdAndName(Long organisationId, String name);
}