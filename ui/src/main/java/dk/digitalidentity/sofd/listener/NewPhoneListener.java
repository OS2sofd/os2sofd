package dk.digitalidentity.sofd.listener;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.EntityChangeQueueDetail;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SupportedUserType;
import dk.digitalidentity.sofd.listener.EntityListenerService.ChangeType;
import dk.digitalidentity.sofd.service.OpusService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import lombok.extern.slf4j.Slf4j;

// on phone change, for persons that has an OPUS account, it will update infotype 105
// if that integration is enabled

@Slf4j
@Component
public class NewPhoneListener implements ListenerAdapter {

	@Autowired
	private PersonService personService;

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private SupportedUserTypeService supportedUserTypeService;
	
	@Autowired
	private OpusService opusService;

	@Override
	public void personUpdated(String uuid, List<EntityChangeQueueDetail> changes) {
		if (!configuration.getModules().getAccountCreation().getOpusHandler().isEnabled()) {
			return;
		}

		List<EntityChangeQueueDetail> phoneChanges = changes.stream().filter(c -> c.getChangeType().equals(ChangeType.CHANGED_PHONES)).collect(Collectors.toList());
		if (phoneChanges.isEmpty()) {
			return;
		}

		SupportedUserType userType = supportedUserTypeService.findByKey(SupportedUserTypeService.getOpusUserType());
		if (userType == null || !userType.isCanOrder()) {
			log.warn("Ordering OPUS accounts has been disabled - updating phones is not possible");
			return;
		}

		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return;
		}
		
		opusService.updatePhones(person);
	}
}
