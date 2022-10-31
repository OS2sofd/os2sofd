package dk.digitalidentity.sofd.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.TagsDao;
import dk.digitalidentity.sofd.dao.model.Tag;

@Service
public class TagsService {

	@Autowired
	private TagsDao tagsDao;

	public List<Tag> findAll() {
		return tagsDao.findAll();
	}

	public Tag findByValue(String tag) {
		return tagsDao.findByValue(tag);
	}
	
	public Tag findById(Long id) {
		return tagsDao.findById(id).orElse(null);
	}

	public Tag save(Tag tag) {
		return tagsDao.save(tag);
	}

	public void delete(Long id) {
		tagsDao.deleteById(id);
	}

	public boolean existsByValue(String value) {
		return tagsDao.existsByValue(value);
	}

}
