package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.Tag;

public interface TagsDao extends CrudRepository<Tag, Long> {
	List<Tag> findAll();

	Tag findByValue(String tag);

	boolean existsByValue(String value);
}
