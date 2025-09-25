package dk.digitalidentity.sofd.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.BadWordsDao;
import dk.digitalidentity.sofd.dao.model.BadWord;

@Service
public class BadWordsService {

	@Autowired
	private BadWordsDao badWordsDao;

	public List<BadWord> findAll() {
		return badWordsDao.findAll();
	}

	public BadWord save(BadWord badWord) {
		return badWordsDao.save(badWord);
	}

	public void delete(Long id) {
		badWordsDao.deleteById(id);
	}

	public BadWord findBadWord(String word) {
		return badWordsDao.findByValue(word);
	}
}
