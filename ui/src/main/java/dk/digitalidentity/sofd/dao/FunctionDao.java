package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.Function;

public interface FunctionDao extends CrudRepository<Function, Long> {

	List<Function> findAll();

	Function findById(long id);
}
