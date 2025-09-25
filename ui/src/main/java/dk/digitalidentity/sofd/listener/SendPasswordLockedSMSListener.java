package dk.digitalidentity.sofd.listener;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.dao.model.EntityChangeQueueDetail;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.Phone;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.enums.PhoneType;
import dk.digitalidentity.sofd.listener.EntityListenerService.ChangeType;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.PwdLockedService;
import dk.digitalidentity.sofd.service.SMSService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SendPasswordLockedSMSListener implements ListenerAdapter {

	@Autowired
	private PersonService personService;

	@Autowired
	private PwdLockedService pwdLockedService;

	@Autowired
	private SMSService smsService;

	@Override
	public void personUpdated(String uuid, List<EntityChangeQueueDetail> changes) {
		if (!pwdLockedService.getPwdLockedEnabled()) {
			return;
		}

		List<EntityChangeQueueDetail> passwordLockedChanges = changes.stream().filter(c -> c.getChangeType().equals(ChangeType.AD_PASSWORD_LOCKED)).collect(Collectors.toList());
		if (passwordLockedChanges.isEmpty()) {
			return;
		}

		Person person = personService.getByUuid(uuid);
		if (person == null) {
			return;
		}

		Phone phone = null;
		if (person.getPhones().isEmpty()) {
			return;
		}
		else {
			List<Phone> filteredPhones = PersonService.getPhones(person).stream().filter(p -> p.getPhoneType() == PhoneType.MOBILE).collect(Collectors.toList());
			Phone primePhone = filteredPhones.stream().filter(Phone::isPrime).findFirst().orElse(null);

			if (primePhone == null && filteredPhones.size() > 0) {
				phone = filteredPhones.get(0);
			}
			else {
				phone = primePhone;
			}
		}

		if (phone == null) {
			return;
		}

		Set<String> phoneNumbers = new HashSet<>();
		phoneNumbers.add(phone.getPhoneNumber());

		for (EntityChangeQueueDetail passwordLockedChange : passwordLockedChanges) {
			User user = PersonService.getUsers(person).stream().filter(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType()) && Objects.equals(u.getMasterId(), passwordLockedChange.getChangeTypeDetails())).findFirst().orElse(null);
			
			if (user != null) {
				String message = pwdLockedService.getPwdReminderSmsTxt();
				message = message.replace("{KONTO}", user.getUserId());
				
				smsService.sendMessage(message, phoneNumbers);
			}
			else {
				log.warn("Unable to find user " + passwordLockedChange.getChangeTypeDetails() + " for person " + person.getUuid());
			}
		}
	}
}