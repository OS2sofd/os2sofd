package dk.digitalidentity.sofd.task;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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

@Component
@EnableScheduling
@Slf4j
public class PwdReminderTask {

	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired
	private PwdReminderService pwdReminderService;
	
	@Autowired
	private PersonService personService;
	
	@Autowired
	private EmailService emailService;
	
	@Autowired
	private SMSService smsService;
	
	// Run every 15 minutes, checking for rules to be executed
	@Scheduled(fixedDelay = 15 * 60 * 1000)
	@Transactional
	public void sendPwdReminders() {
		if (!configuration.getScheduled().isEnabled() || !configuration.getModules().getSmsGateway().isPwdReminderEnabled()) {
			return;
		}
		
		PwdReminderStrategy strategy = pwdReminderService.getPwdReminderStrategy();
		if (strategy.equals(PwdReminderStrategy.DISABLED)) {
			return;
		}
		
		LocalTime time = pwdReminderService.getPwdReminderTime();
		LocalTime now = LocalTime.now();
		LocalTime nowPlus15Minutes = now.plusMinutes(14).plusSeconds(59);
		
		if (time.isAfter(now) && time.isBefore(nowPlus15Minutes)) {
			run(strategy);
		}
	}

	private void run(PwdReminderStrategy strategy) {
		Set<Long> days = getDays();
		
		log.info("Running PwdReminderTask");
		
		String emailTxt = pwdReminderService.getPwdReminderEmailTxt();
		String emailSubject = pwdReminderService.getPwdReminderEmailSubject();
		String smsTxt = pwdReminderService.getPwdReminderSmsTxt();
		
		Set<String> ouFilter = pwdReminderService.getPwdReminderOrgUnitFilter();
		
		LocalDate today = LocalDate.now();
		for (long day : days) {
			LocalDate expiryDate = today.plusDays(day);
			
			List<Person> persons = personService.findAllWithADPwdChangePending(expiryDate.toString());

			log.info("Found " + persons.size() + " persons that expire on: " + expiryDate);
			
			for (Person person : persons) {
				if (isFiltered(person, ouFilter)) {
					continue;
				}

				for (User user : PersonService.getUsers(person)) {
					if (!SupportedUserTypeService.isActiveDirectory(user.getUserType())) {
						continue;
					}
					
					ActiveDirectoryDetails activeDirectoryDetails = user.getActiveDirectoryDetails();
					if (activeDirectoryDetails == null) {
						continue;
					}

					if (activeDirectoryDetails.getPasswordExpireDate() == null || !activeDirectoryDetails.getPasswordExpireDate().equals(expiryDate)) {
						continue;
					}
					
					switch (strategy) {
						case DISABLED:
							break;
						case SMS_ONLY:
							notifySms(person, user, smsTxt, day);
							break;
						case EMAIL_ONLY:
							notifyEmail(person, user, emailTxt, emailSubject, day);
							break;
						case SMS_AND_EMAIL:
							notifySms(person, user, smsTxt, day);
							notifyEmail(person, user, emailTxt, emailSubject, day);
							break;
						case EMAIL_FIRST_OTHERWISE_SMS:
							if (!notifyEmail(person, user, emailTxt, emailSubject, day)) {
								notifySms(person, user, smsTxt, day);
							}
							break;
						case SMS_FIRST_OTHERWISE_EMAIL:
							if (!notifySms(person, user, emailTxt, day)) {
								notifyEmail(person, user, smsTxt, emailSubject, day);
							}
							break;
					}
				}
			}
		}
	}
	
	private boolean isFiltered(Person person, Set<String> ouFilter) {
		if (ouFilter.size() > 0) {
			Set<String> orgUnitUuids = person.getAffiliations().stream().map(a -> a.getOrgUnit().getUuid()).collect(Collectors.toSet());
			orgUnitUuids.addAll(person.getAffiliations().stream().filter(a -> a.getAlternativeOrgUnit() != null).map(a -> a.getAlternativeOrgUnit().getUuid()).collect(Collectors.toSet()));
			orgUnitUuids.addAll(findWorkplaceOUUuids(person.getAffiliations()));

			// find intersection between ouFilter-set and the orgunits that the user is in
			orgUnitUuids.retainAll(ouFilter);
			
			// if no overlap, the user is filtered (no message send)
			return (orgUnitUuids.size() == 0);
		}

		// no filter, no one is filtered...
		return false;
	}

	private Set<String> findWorkplaceOUUuids(List<Affiliation> affiliations) {
		Set<String> orgUnitUuids = new HashSet<>();
		for (Affiliation affiliation : affiliations) {
			if (affiliation.getWorkplaces() != null) {
				orgUnitUuids.addAll(affiliation.getWorkplaces().stream().map(w -> w.getOrgUnit().getUuid()).collect(Collectors.toSet()));
			}
		}
		return orgUnitUuids;
	}

	private boolean notifyEmail(Person person, User user, String emailTxt, String subject, long days) {
		Optional<User> emailUser = PersonService.getUsers(person).stream().filter(u -> SupportedUserTypeService.isExchange(u.getUserType()) && u.isPrime()).findAny();
		
		if (emailUser.isPresent()) {
			String emailAddress = emailUser.get().getUserId();
			
			emailTxt = emailTxt.replace("{NAVN}", PersonService.getName(person))
							   .replace("{DAGE}", ((days > 1) ? (days + " dage") : "1 dag"))
							   .replace("{KONTO}", user.getUserId());

			emailService.sendMessage(emailAddress, subject, emailTxt, null, null, null, "Påmånedelse om kodeordudløb for konto " + user.getUserId());
			
			return true;
		}

		return false;
	}

	private boolean notifySms(Person person, User user, String smsTxt, long days) {
		Optional<Phone> mobile = PersonService.getPhones(person).stream().filter(p -> p.getPhoneType().equals(PhoneType.MOBILE) && p.isTypePrime()).findAny();
		
		if (mobile.isPresent()) {
			String phoneNumber = mobile.get().getPhoneNumber();
			
			smsTxt = smsTxt.replace("{NAVN}", PersonService.getName(person))
						   .replace("{DAGE}", ((days > 1) ? (days + " dage") : "1 dag"))
						   .replace("{KONTO}", user.getUserId());

			log.info("Sending password reminder on SMS to " + person.getUuid() + " (" + user.getUserId() + ")");

			smsService.sendMessage(smsTxt, Collections.singleton(phoneNumber));

			return true;
		}
		
		return false;
	}

	private Set<Long> getDays() {
		String daysString = pwdReminderService.getPwdReminderDaysBefore();
		String[] dayTokens = daysString.split(",");
		Set<Long> days = new HashSet<>();
		for (String day : dayTokens) {
			try {
				days.add(Long.parseLong(day));
			}
			catch (Exception ex) {
				log.error("Could not parse day:" + day);
			}
		}

		return days;
	}
}
