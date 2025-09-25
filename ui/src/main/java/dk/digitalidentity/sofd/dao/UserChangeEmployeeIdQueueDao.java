package dk.digitalidentity.sofd.dao;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.UserChangeEmployeeIdQueue;

public interface UserChangeEmployeeIdQueueDao extends CrudRepository<UserChangeEmployeeIdQueue, Long> {
	List<UserChangeEmployeeIdQueue> findAllByDateOfTransaction(LocalDate dateOfTransaction);
	UserChangeEmployeeIdQueue findByUser(User user);
}
