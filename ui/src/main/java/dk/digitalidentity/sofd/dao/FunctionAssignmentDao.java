package dk.digitalidentity.sofd.dao;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.FunctionAssignment;

public interface FunctionAssignmentDao extends CrudRepository<FunctionAssignment, Long> {

	List<FunctionAssignment> findAll();
	FunctionAssignment findById(long id);
	List<FunctionAssignment> findByStopDate(LocalDate stopDate);
	List<FunctionAssignment> findByAffiliation(Affiliation affiliation);
	@Query(nativeQuery = true, value = "SELECT DISTINCT fa.* \n" +
			"FROM fh_function_assignment fa\n" +
			"INNER JOIN fh_function f ON f.id = fa.function_id\n" +
			"INNER JOIN fh_function_facet ff on ff.function_id = f.id\n" +
			"INNER JOIN fh_facet fac ON fac.id = ff.facet_id AND fac.TYPE = 'FOLLOW_UP_DATE'\n" +
			"INNER JOIN fh_facet_value fval ON fval.function_assignment_id = fa.id AND fval.facet_id = fac.id AND fval.date = curdate()")
	List<FunctionAssignment> getFollowUpAssignments();

}
