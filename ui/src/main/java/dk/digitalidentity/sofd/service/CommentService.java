package dk.digitalidentity.sofd.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.CommentDao;
import dk.digitalidentity.sofd.dao.model.Comment;

@Service
public class CommentService {

	@Autowired
	private CommentDao commentDao;

	public Comment save(Comment newComment) {
		return commentDao.save(newComment);
	}

	public List<Comment> findByPersonUuid(String uuid) {
		return commentDao.findByPersonUuid(uuid);
	}

}
