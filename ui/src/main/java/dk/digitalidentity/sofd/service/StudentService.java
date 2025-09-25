package dk.digitalidentity.sofd.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.StudentDao;
import dk.digitalidentity.sofd.dao.model.Student;

@Service
public class StudentService {

	@Autowired
	private StudentDao studentDao;

	public List<Student> getAll() {
		return studentDao.findAll();
	}

	public Student save(Student student) {
		return studentDao.save(student);
	}

	public Page<Student> getAll(Pageable pageable) {
		return studentDao.findAll(pageable);
	}
	
	public Student findByCpr(String cpr) {
		return studentDao.findByCpr(cpr);
	}

	public Student findByUserId(String userId) {
		return studentDao.findByUserId(userId);
	}
	
	public Student getByUuid(String uuid) {
		return studentDao.findByUuid(uuid);
	}
	
	public void delete(Student student) {
		studentDao.delete(student);
	}

	public List<Student> getByOffsetAndLimit(long offset, int size) {
		if (offset > 0) {
			return studentDao.findLimitedWithOffset(size, offset);
		}

		return studentDao.findLimited(size);
	}
}
