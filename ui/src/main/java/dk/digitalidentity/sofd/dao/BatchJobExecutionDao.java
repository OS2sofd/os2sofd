package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.BatchJobExecution;

public interface BatchJobExecutionDao extends CrudRepository<BatchJobExecution, Long> {

	List<BatchJobExecution> findAll();

	BatchJobExecution findByJobName(String name);

}