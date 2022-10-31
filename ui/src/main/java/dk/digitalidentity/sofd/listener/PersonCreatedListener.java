package dk.digitalidentity.sofd.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.service.AuthorizationCodeService;
import dk.digitalidentity.sofd.service.PersonService;

@Component
public class PersonCreatedListener implements ListenerAdapter {
	
	@Autowired
	private PersonService personService;
	
	@Autowired
	private AuthorizationCodeService authorizationCodeService;
	
	@Override
	public void personCreated(String uuid) {
		Person person = personService.getByUuid(uuid);
		
		if (person != null) {
			authorizationCodeService.syncAuthorizationCode(person);
		}	
	}
}
