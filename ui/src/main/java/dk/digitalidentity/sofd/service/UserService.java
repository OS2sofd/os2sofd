package dk.digitalidentity.sofd.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.UserDao;
import dk.digitalidentity.sofd.dao.model.User;

@Service
public class UserService {
	// same as the default in OS2sync configuration class, just for easy reading in this class
	private static String substituteRegex = "^vik\\d+$";

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private UserDao userDao;
	
	@EventListener(ApplicationReadyEvent.class)
	public void runOnStartup() {
		substituteRegex = configuration.getIntegrations().getOs2sync().getSubstituteRegex();
	}

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
	
	public List<User> findByUserTypeAndDisabledFalse(String userType) {
		return userDao.findByUserTypeAndDisabledFalse(userType);
	}

	public static boolean isSubstituteUser(User user) {
		boolean result = false;
		
		// AD user check
		result |= SupportedUserTypeService.isActiveDirectory(user.getUserType()) && user.getUserId().toLowerCase().matches(substituteRegex);

		// Exchange user check
		result |= SupportedUserTypeService.isExchange(user.getUserType()) && user.getMasterId().toLowerCase().matches(substituteRegex);

		return result;
	}
}
