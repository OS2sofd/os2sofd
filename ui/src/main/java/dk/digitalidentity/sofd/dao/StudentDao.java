package dk.digitalidentity.sofd.dao;

import dk.digitalidentity.sofd.dao.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentDao extends JpaRepository<Student, Long> {
	Student findByCpr(String cpr);
	Student findByUsername(String username);
	Student findByUuid(String uuid);

	void deleteByUsername(String username);
}
