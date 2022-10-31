package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.BadWord;

public interface BadWordsDao extends CrudRepository<BadWord, Long> {
	List<BadWord> findAll();

	BadWord findByValue(String word);
}
