package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.Comment;

public interface CommentDao extends CrudRepository<Comment, Long> {
	List<Comment> findByPersonUuid(String personUuid);
}