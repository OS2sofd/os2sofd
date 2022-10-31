package dk.digitalidentity.sofd.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.SubstituteAssignmentDao;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SubstituteAssignment;

@Service
public class SubstituteAssignmentService {

	@Autowired
	private SubstituteAssignmentDao substituteAssignmentDao;

	public SubstituteAssignment getById(long id) {
		return substituteAssignmentDao.findById(id);
	}

	public SubstituteAssignment save(SubstituteAssignment assignment) {
		assignment.setChanged(LocalDateTime.now());
		return substituteAssignmentDao.save(assignment);
	}

	public void delete(SubstituteAssignment assignment) {
		substituteAssignmentDao.delete(assignment);
	}

	public List<SubstituteAssignment> findAll() {
		return substituteAssignmentDao.findAll();
	}

	public List<SubstituteAssignment> findBySubstitute(Person person) {
		return substituteAssignmentDao.findBySubstitute(person);
	}
}
