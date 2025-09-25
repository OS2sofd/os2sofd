package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SubstituteAssignment;

public interface SubstituteAssignmentDao extends JpaRepository<SubstituteAssignment, Long> {
	SubstituteAssignment findById(long id);

	List<SubstituteAssignment> findBySubstitute(Person person);
}
