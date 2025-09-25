package dk.digitalidentity.sofd.dao;

import dk.digitalidentity.sofd.dao.model.EmailTemplate;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface EmailTemplateChildDao extends CrudRepository<EmailTemplateChild, Long> {
	List<EmailTemplateChild> findByEmailTemplate(EmailTemplate emailTemplate);
	EmailTemplateChild findById(long id);

	@Query(nativeQuery = true, value = """
			with recursive cte as
			(
				select
					etco.org_unit_uuid
				from email_template_children etc
				inner join email_template_child_org_unit etco on etco.email_template_child_id = etc.id
				where etc.id = ?1
			
				union all
			
				select
					o.uuid as org_unit_uuid
				from orgunits o
				inner join cte on ?3 and cte.org_unit_uuid = o.parent_uuid
			)
			select case when count(*) > 0 then 'true' else 'false' end from cte
			where org_unit_uuid = ?2
			""")
	boolean isInFilteredSet(Long childId, String orgUnitUuid, boolean includeChildren);
}
