package dk.digitalidentity.sofd.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.sofd.dao.KnownUsernamesDao;
import dk.digitalidentity.sofd.dao.model.KnownUsername;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.service.transliteration.Transliteration;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class KnownUsernamesService {

	@Autowired
	private KnownUsernamesDao knownUsernamesDao;
	
	@Autowired
	private UserService userService;

	public List<KnownUsername> findAll() {
		return knownUsernamesDao.findAll();
	}

	public KnownUsername findByUsernameAndUserType(String username, String userType) {
		return knownUsernamesDao.findByUsernameAndUserType(username, userType);
	}

	public List<KnownUsername> findByUsername(String username) {
		return knownUsernamesDao.findByUsernameIgnoreCase(username);
	}

	public void save(List<KnownUsername> entities) {
		knownUsernamesDao.saveAll(entities);
	}
	
	@Transactional(rollbackFor = Exception.class)
	public void findNewUsernames() {
		List<KnownUsername> usernamesToBeAdded = new ArrayList<KnownUsername>();
		List<KnownUsername> knownUsernames = findAll();
		List<User> users = userService.getAll();

		for (User user : users) {
			String username = user.getUserId();
			String usertype = user.getUserType();

			// strip suffix from email addresses
			if (SupportedUserTypeService.isExchange(usertype)) {
				int idx = username.indexOf("@");
				if (idx > 0) {
					username = username.substring(0, idx);
				}
			}

			String safeUsername = Transliteration.transliterate(username, null).replaceAll("\\s+","");

			if (!knownUsernames.stream().anyMatch(ku -> ku.getUsername().equalsIgnoreCase(safeUsername) && ku.getUserType().equals(usertype)) &&
				!usernamesToBeAdded.stream().anyMatch(ku -> ku.getUsername().equalsIgnoreCase(safeUsername) && ku.getUserType().equals(usertype))) {

				KnownUsername newKnownUsername = new KnownUsername();
				newKnownUsername.setUsername(safeUsername);
				newKnownUsername.setUserType(usertype);

				usernamesToBeAdded.add(newKnownUsername);
			}
		}

		if (usernamesToBeAdded.size() > 0) {
			log.info("Adding " + usernamesToBeAdded.size() + " new usernames to list of known usernames");
			try {
				save(usernamesToBeAdded);
			}
			catch (Exception ex) {
				log.error("Failed to save new known usernames - check next log entry for details", ex);
				for (KnownUsername username : usernamesToBeAdded) {
					log.info(username.getUserType() + " / " + username.getUsername());
				}
			}
		}
	}
}
