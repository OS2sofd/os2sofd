package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.EmailTemplate;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;

public interface EmailTemplateChildDao extends CrudRepository<EmailTemplateChild, Long> {
	List<EmailTemplateChild> findByEmailTemplate(EmailTemplate emailTemplate);
	EmailTemplateChild findById(long id);
}
