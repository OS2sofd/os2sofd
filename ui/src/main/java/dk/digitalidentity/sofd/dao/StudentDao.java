package dk.digitalidentity.sofd.dao;

import dk.digitalidentity.sofd.dao.model.Student;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StudentDao extends JpaRepository<Student, Long> {
	Student findByCpr(String cpr);
	Student findByUserId(String userId);
	Student findByUuid(String uuid);
	
	@Query(nativeQuery = true, value = "SELECT * FROM student WHERE id > :offset ORDER BY id LIMIT :size")
	List<Student> findLimitedWithOffset(int size, long offset);

	@Query(nativeQuery = true, value = "SELECT * FROM student ORDER BY id LIMIT :size")
	List<Student> findLimited(int size);
}
