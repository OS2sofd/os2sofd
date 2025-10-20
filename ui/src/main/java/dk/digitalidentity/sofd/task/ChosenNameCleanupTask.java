package dk.digitalidentity.sofd.task;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.ActiveDirectoryDetails;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.Phone;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.enums.PhoneType;
import dk.digitalidentity.sofd.service.EmailService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.PwdReminderService;
import dk.digitalidentity.sofd.service.SMSService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import dk.digitalidentity.sofd.service.model.PwdReminderStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@EnableScheduling
@Slf4j
public class ChosenNameCleanupTask {

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private PersonService personService;

	// run once per day
	@Scheduled(fixedDelay = 24 * 60 * 60 * 1000)
	@Transactional
	public void cleanupChosenNames() {
		if (!configuration.getScheduled().isEnabled() || !configuration.getModules().getPerson().isResetChosenNameWhenInactive()) {
			return;
		}

		personService.removeChosenNameOnInactivePersons();
	}
}
