package dk.digitalidentity.sofd.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.sofd.dao.UserChangeEmployeeIdQueueDao;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.UserChangeEmployeeIdQueue;

@Service
public class UserChangeEmployeeIdQueueService {
	
	@Autowired
	private UserChangeEmployeeIdQueueDao userChangeEmployeeIdQueueDao;
	
	@Autowired 
	private UserService userService;
	
	public UserChangeEmployeeIdQueue findByUser(User user) {
		return userChangeEmployeeIdQueueDao.findByUser(user);
	}

	public UserChangeEmployeeIdQueue save(UserChangeEmployeeIdQueue userChangeEmployeeIdQueue) {
		return userChangeEmployeeIdQueueDao.save(userChangeEmployeeIdQueue);
	}
	
	public void delete(UserChangeEmployeeIdQueue queue) {
		userChangeEmployeeIdQueueDao.delete(queue);
	}

	@Transactional
	public void handleChanges() {
		// TODO: også senere, hvis vi skipper en dag
		List<UserChangeEmployeeIdQueue> changes = userChangeEmployeeIdQueueDao.findAllByDateOfTransaction(LocalDate.now());
		
		for (UserChangeEmployeeIdQueue change : changes) {
			User user = change.getUser();
			user.setEmployeeId(change.getEmployeeId());
			userService.save(user);
		}
		
		userChangeEmployeeIdQueueDao.deleteAll(changes);
	}
}
