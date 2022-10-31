package dk.digitalidentity.sofd.task;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.KnownUsername;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.service.KnownUsernamesService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import dk.digitalidentity.sofd.service.UserService;
import dk.digitalidentity.sofd.service.transliteration.Transliteration;
import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class KnownUsernamesTask {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private KnownUsernamesService knownUsernamesService;

	@Autowired
	private UserService userService;

	// TODO: can we add a fuzz-factor, so not ALL instances run at the exact same time?
	// run every day at 01:00
	@Scheduled(cron = "0 0 1 * * *")
	@Transactional(rollbackFor = Exception.class)
	public void findNewUsernames() {
		if (!configuration.getScheduled().isEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}

		List<KnownUsername> usernamesToBeAdded = new ArrayList<KnownUsername>();
		List<KnownUsername> knownUsernames = knownUsernamesService.findAll();
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
				knownUsernamesService.save(usernamesToBeAdded);
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
