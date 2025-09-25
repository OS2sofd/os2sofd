package dk.digitalidentity.sofd.dao;

import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SubstituteContext;
import dk.digitalidentity.sofd.dao.model.SubstituteOrgUnitAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SubstituteOrgUnitAssignmentDao extends JpaRepository<SubstituteOrgUnitAssignment, Long> {
	SubstituteOrgUnitAssignment getById(long id);
	List<SubstituteOrgUnitAssignment> findBySubstitute(Person person);
	List<SubstituteOrgUnitAssignment> findByContext(SubstituteContext context);

	@Query(nativeQuery = true, value = """
		-- this recursive cte starts with the given orgUnitUuid and fetches all ancestors
		with recursive cte as
		(
			select
				o.uuid,
				o.parent_uuid,
				o.uuid as child
			from orgunits o
			where
				o.uuid = :orgUnitUuid
				and deleted = 0
			union all
			select
				o.uuid,
				o.parent_uuid,
				cte.child as child
			from orgunits o
			inner join cte on cte.parent_uuid = o.uuid
			where
				o.deleted = 0
		)
		select distinct substitute_uuid from
		substitute_context c
		inner join substitute_org_unit_assignment a on a.substitute_context_id = c.id
		inner join cte on
			cte.child = a.org_unit_uuid -- direct substitute
			or (cte.uuid = a.org_unit_uuid and c.inherit_org_unit_assignments = 1) -- inherited substitute
		where
			c.identifier in ('SOFD','GLOBAL')
	""")
	List<String> getSofdSubstituteUuids(@Param("orgUnitUuid") String orgUnitUuid);
}
