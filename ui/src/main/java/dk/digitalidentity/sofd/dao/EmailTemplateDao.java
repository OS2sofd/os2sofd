package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.EmailTemplate;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplateType;

public interface EmailTemplateDao extends CrudRepository<EmailTemplate, Long> {
	EmailTemplate findByTemplateType(EmailTemplateType type);
	EmailTemplate findById(long id);
	List<EmailTemplate> findAll();

	boolean existsByTemplateType(EmailTemplateType type);
}
