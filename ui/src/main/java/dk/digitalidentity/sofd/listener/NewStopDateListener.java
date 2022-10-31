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

// on stopDate change (affiliation), for persons that has an OPUS account, it will update infotype 105
// if that integration is enabled

@Component
public class NewStopDateListener implements ListenerAdapter {

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

		SupportedUserType userType = supportedUserTypeService.findByKey(SupportedUserTypeService.getOpusUserType());
		if (userType == null || !supportedUserTypeService.findByKey(SupportedUserTypeService.getOpusUserType()).isCanOrder()) {
			return;
		}

		List<EntityChangeQueueDetail> affiliationChanges = changes.stream().filter(c -> c.getChangeType().equals(ChangeType.CHANGED_AFFILIATION_STOP_DATE)).collect(Collectors.toList());
		if (affiliationChanges.isEmpty()) {
			return;
		}

		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return;
		}
		
		if (PersonService.getUsers(person).stream().noneMatch(u -> u.getUserType().equals(userType.getKey()))) {
			return;
		}
		
		for (EntityChangeQueueDetail change : affiliationChanges) {
			opusService.updateStopDate(person, change.getChangeTypeDetails());
		}
	}
}
