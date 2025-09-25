package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.Kle;

public interface KleDao extends CrudRepository<Kle, Long> {
	
	List<Kle> findAll();

	List<Kle> findAllByParent(String parent);

	Kle getByCode(String code);

	long countByActiveTrue();
}
