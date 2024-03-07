package dk.digitalidentity.sofd.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.UserDao;
import dk.digitalidentity.sofd.dao.model.User;

@Service
public class UserService {

	@Autowired
	private UserDao userDao;

	public List<User> getAll() {
		return userDao.findAll();
	}
	
	public void save(User user) {
		userDao.save(user);
	}

	public User findByUserIdAndUserType(String userId, String userType) {
		return userDao.findByUserIdAndUserType(userId, userType);
	}

	public User findByUserIdAndUserTypeAndMaster(String userId, String userType, String master) {
		return userDao.findByUserIdAndUserTypeAndMaster(userId, userType, master);
	}

	public User findByUuid(String uuid) {
		return userDao.findByUuid(uuid);
	}

	public List<User> findByUserIdLikeAndUserType(String word, String userType) {
		return userDao.findByUserIdLikeAndUserType(word, userType);
	}

	public static boolean isSubstituteADUser(User user) {
		var substituteRegex = "^vik\\d+$";
		return SupportedUserTypeService.isActiveDirectory(user.getUserType()) && user.getUserId().toLowerCase().matches(substituteRegex);
	}
}
