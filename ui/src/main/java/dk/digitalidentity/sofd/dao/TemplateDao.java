package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.Template;

public interface TemplateDao extends CrudRepository<Template, Long> {
	List<Template> findAll();
}
