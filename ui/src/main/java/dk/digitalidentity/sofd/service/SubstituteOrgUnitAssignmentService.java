package dk.digitalidentity.sofd.service;

import dk.digitalidentity.sofd.dao.SubstituteOrgUnitAssignmentDao;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SubstituteContext;
import dk.digitalidentity.sofd.dao.model.SubstituteOrgUnitAssignment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SubstituteOrgUnitAssignmentService {

	@Autowired
	private SubstituteOrgUnitAssignmentDao substituteOrgUnitAssignmentDao;

	public SubstituteOrgUnitAssignment getById(long id) {
		return substituteOrgUnitAssignmentDao.getById(id);
	}

	public void delete(SubstituteOrgUnitAssignment assignment) {
		substituteOrgUnitAssignmentDao.delete(assignment);
	}

	public SubstituteOrgUnitAssignment save(SubstituteOrgUnitAssignment assignment) {
		assignment.setChanged(LocalDateTime.now());
		return substituteOrgUnitAssignmentDao.save(assignment);
	}
	public List<SubstituteOrgUnitAssignment> findBySubstitute(Person person) {
		return substituteOrgUnitAssignmentDao.findBySubstitute(person);
	}
	public List<SubstituteOrgUnitAssignment> findByContext(SubstituteContext context) {
		return substituteOrgUnitAssignmentDao.findByContext(context);
	}

	public List<SubstituteOrgUnitAssignment> getAll() {
		return substituteOrgUnitAssignmentDao.findAll();
	}
	public void deleteAllByContext(SubstituteContext context) {
		substituteOrgUnitAssignmentDao.deleteAll(findByContext(context));
	}
}
