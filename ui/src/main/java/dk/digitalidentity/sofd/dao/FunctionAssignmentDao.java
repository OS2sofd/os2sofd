package dk.digitalidentity.sofd.dao;

import java.util.Date;
import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.FunctionAssignment;

public interface FunctionAssignmentDao extends CrudRepository<FunctionAssignment, Long> {

	List<FunctionAssignment> findAll();
	FunctionAssignment findById(long id);
	List<FunctionAssignment> findByStopDateBetween(Date start, Date Stop);
	List<FunctionAssignment> findByAffiliation(Affiliation affiliation);
}
