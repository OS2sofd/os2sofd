package dk.digitalidentity.sofd.dao;

import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SubstituteContext;
import dk.digitalidentity.sofd.dao.model.SubstituteOrgUnitAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubstituteOrgUnitAssignmentDao extends JpaRepository<SubstituteOrgUnitAssignment, Long> {
	SubstituteOrgUnitAssignment getById(long id);
	List<SubstituteOrgUnitAssignment> findBySubstitute(Person person);
	List<SubstituteOrgUnitAssignment> findByContext(SubstituteContext context);
}
