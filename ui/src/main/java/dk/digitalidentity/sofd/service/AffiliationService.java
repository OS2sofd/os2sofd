package dk.digitalidentity.sofd.service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import dk.digitalidentity.sofd.dao.model.*;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.AffiliationDao;
import dk.digitalidentity.sofd.dao.model.enums.AffiliationFunction;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplateType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@EnableScheduling
public class AffiliationService {

	@Autowired
	private AffiliationDao affiliationDao;
	
	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired
	private EmailTemplateService emailTemplateService;
	
	@Autowired
	private EmailQueueService emailQueueService;
	
	@Autowired
	private SubstituteAssignmentService substituteAssignmentService;

	public List<Affiliation> findAll() {
		return affiliationDao.findAll();
	}

	public List<Affiliation> findByMaster(String master) {
		return affiliationDao.findByMaster(master);
	}

	public Affiliation findByUuid(String uuid) {
		return affiliationDao.findByUuid(uuid);
	}

	public Affiliation save(Affiliation affiliation) {
		return affiliationDao.save(affiliation);
	}

	public Long countByOrgUnitAndActive(OrgUnit orgUnit) {
		return affiliationDao.countByOrgUnitAndActive(orgUnit.getUuid());
	}

	public List<Affiliation> findByOrgUnitAndActive(OrgUnit orgUnit) {
		return affiliationDao.findByOrgUnitAndActive(orgUnit.getUuid());
	}
	
	public void delete(Affiliation affiliation) {
		affiliationDao.delete(affiliation);
	}
	
	public static List<AffiliationFunction> getFunctions(Affiliation affiliation) {
		if (affiliation.getFunctions() == null) {
			return new ArrayList<>();
		}
		
		return affiliation.getFunctions().stream().map(f -> f.getFunction()).collect(Collectors.toList());
	}
	
	public static List<String> getKlePrimary(Affiliation affiliation) {
		if (affiliation.getKlePrimary() == null) {
			return new ArrayList<>();
		}
		
		return affiliation.getKlePrimary().stream().map(k -> k.getKleValue()).collect(Collectors.toList());
	}
	
	public static List<String> getKleSecondary(Affiliation affiliation) {
		if (affiliation.getKleSecondary() == null) {
			return new ArrayList<>();
		}
		
		return affiliation.getKleSecondary().stream().map(k -> k.getKleValue()).collect(Collectors.toList());
	}
	
	public static List<OrgUnit> getManagerFor(Affiliation affiliation) {
		if (affiliation.getManagerFor() == null) {
			return new ArrayList<>();
		}
		
		return affiliation.getManagerFor().stream().map(a -> a.getOrgUnit()).collect(Collectors.toList());
	}

	public static String getPositionName(Affiliation affiliation) {
		if (affiliation.getPositionDisplayName() != null && affiliation.getPositionDisplayName().length() > 0) {
			return affiliation.getPositionDisplayName();
		}

		return affiliation.getPositionName();
	}
	
	// helper methods to decide if an affiliation is active
	
	public static List<Affiliation> onlyActiveAffiliations(List<Affiliation> affiliations) {
		return onlyActiveAffiliations(affiliations, 0);
	}
	
	public static List<Affiliation> notStoppedAffiliations(List<Affiliation> affiliations) {
		return CollectionUtils.emptyIfNull(affiliations).stream()
				.filter(a ->
						(!a.isDeleted()) && // TODO: goes away at some point
						(!notActiveAnymore(a)))
				.collect(Collectors.toList());
	}
	
	public static List<Affiliation> onlyActiveAffiliations(List<Affiliation> affiliations, int offsetDays) {
		return CollectionUtils.emptyIfNull(affiliations).stream()
				.filter(a ->
						(!a.isDeleted()) && // TODO: goes away at some point
						(!notActiveYet(a, offsetDays)) &&
						(!notActiveAnymore(a)))
				.collect(Collectors.toList());
	}

	public static List<Affiliation> allAffiliationsActiveSinceMonths(List<Affiliation> affiliations, int months) {
		return affiliations.stream()
				.filter(a ->
						(!a.isDeleted()) && // TODO: goes away at some point
						(!notActiveYet(a, 30 * months)) && // yeah yeah, 30 days per month is not exact, but close enough
						(!notActiveAnymoreSinceMonthsAgo(a, months)))
				.collect(Collectors.toList());
	}

	// TODO: we removed the "stopped" marker, so we can cleanup this stuff at some point

	public static boolean inactiveAndNotStopped(Affiliation affiliation) {
		return inactiveAndNotStopped(affiliation, 0);
	}

	public static boolean inactiveAndNotStopped(Affiliation affiliation, int offsetDays) {
		if (notActiveYet(affiliation, offsetDays) || notActiveAnymore(affiliation)) {
			return true;
		}
		
		return false;
	}

	public static boolean activeAndNotStopped(Affiliation affiliation) {
		return activeAndNotStopped(affiliation, 0);
	}

	public static boolean activeAndNotStopped(Affiliation affiliation, int offsetDays) {
		if (!notActiveYet(affiliation, offsetDays) && !notActiveAnymore(affiliation)) {
			return true;
		}
		
		return false;
	}

	/**
	 * The offsetDays should be >= 0, and is subtracted from "today", when deciding if a given
	 * affiliation is still active, allowing for affiliations that becomes active within the
	 * next "offsetDays" to be treated as active
	 */
	public static boolean notActiveYet(Affiliation affiliation, int offsetDays) {
		Date today = getToday();
		
		if (offsetDays > 0) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(today);
			cal.add(Calendar.DATE, (offsetDays));
			today = cal.getTime();
		}

		return (affiliation.getStartDate() != null && affiliation.getStartDate().after(today)); 
	}

	public static boolean notActiveAnymore(Affiliation affiliation) {
		return (affiliation.getStopDate() != null && affiliation.getStopDate().before(getYesterday()));
	}

	public static boolean notActiveAnymoreSinceMonthsAgo(Affiliation affiliation, int months) {
		return (affiliation.getStopDate() != null && affiliation.getStopDate().before(getMonthsAgo(months)));
	}
	
	private LocalDate toLocalDate(Date date) {
		if (date == null) {
			return null;
		}
		
		// might be an SQL instance, so convert to something that has a toInstant() method on it
		if (date instanceof java.sql.Date) {
			date = new Date(date.getTime());
		}

	    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}
	
	private void processSubstituteStopsTemplate(List<Affiliation> affiliations, EmailTemplateChild child, List<SubstituteAssignment> substituteAssignments) {
		for (Affiliation affiliation : affiliations) {
			List<SubstituteAssignment> subs = substituteAssignments.stream()
					.filter(a -> Objects.equals(a.getSubstitute().getUuid(), affiliation.getPerson().getUuid()))
					.collect(Collectors.toList());

			for (SubstituteAssignment substituteAssignment : subs) {
				Person manager = substituteAssignment.getPerson();
				Person substitute = substituteAssignment.getSubstitute();
	
				String email = PersonService.getEmail(manager);
				if (!StringUtils.hasLength(email)) {
					log.warn("processSubstituteStopsTemplate - no email address found.");
					continue;
				}
	
				String message = child.getMessage();
				message = message.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, PersonService.getName(manager));
				message = message.replace(EmailTemplateService.SUBSTITUTE_PLACEHOLDER, PersonService.getName(substitute));
				message = message.replace(EmailTemplateService.SUBSTITUTE_CONTEXT_PLACEHOLDER, substituteAssignment.getContext().getName());
				
				String title = child.getTitle();
				title = title.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, PersonService.getName(manager));
				title = title.replace(EmailTemplateService.SUBSTITUTE_PLACEHOLDER, PersonService.getName(substitute));
				title = title.replace(EmailTemplateService.SUBSTITUTE_CONTEXT_PLACEHOLDER, substituteAssignment.getContext().getName());
				
				emailQueueService.queueEmail(email, title, message, 0, child);
			}
		}
	}



	private void processResignationTemplate(List<Affiliation> affiliations, EmailTemplateChild child, EmailTemplate template) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

		for (Affiliation affiliation : affiliations) {
			OrgUnitManager manager = affiliation.getOrgUnit().getManager();
			if (manager == null) {
				log.warn("OrgUnit " + affiliation.getOrgUnit().getName() + " does not have a manager");
				continue;
			}
			
			if (configuration.getEmailTemplate().isOrgFilterEnabled() && template.getTemplateType().isShowOrgFilter()) {
				List<String> excludedOUUuids = child.getExcludedOrgUnitMappings().stream().map(o -> o.getOrgUnit()).map(o -> o.getUuid()).collect(Collectors.toList());
				if (excludedOUUuids.contains(affiliation.getOrgUnit().getUuid())) {
					log.info("Not sending email for email template child with id " + child.getId() + " for affiliation with uuid " + affiliation.getUuid() + ". The affiliation OU was in the excluded ous list");
					continue;
				}
			}
			
			if (configuration.getEmailTemplate().isAdUserFilterEnabled() && template.getTemplateType().isShowADUserFilter() && child.isAdRequired()) {
				Person person = affiliation.getPerson();
				List<User> adUsers = person.getUsers().stream().map(m -> m.getUser()).filter(u -> !u.isDisabled() && SupportedUserTypeService.isActiveDirectory(u.getUserType())).collect(Collectors.toList());
				long matchCount = adUsers.stream().filter(u -> (u.getEmployeeId() != null && u.getEmployeeId().equals(affiliation.getEmployeeId())) || u.getEmployeeId() == null ).count();
				
				if (matchCount == 0) {
					continue;
				}
			}
			
			List<String> emailRecipients = emailTemplateService.getManagerOrSubstitutes(child, manager.getManager(), affiliation.getOrgUnit().getUuid());

			if (CollectionUtils.isEmpty(emailRecipients)) {
				log.warn("ProcessAffiliation - no email address found.");
				continue;
			}
			String userId = getUserIdForAffiliation( affiliation);

			String message = child.getMessage();
			message = message.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, manager.getName());
			message = message.replace(EmailTemplateService.ORGUNIT_PLACEHOLDER, affiliation.getOrgUnit().getName());
			message = message.replace(EmailTemplateService.TIMESTAMP_PLACEHOLDER, dateFormat.format(affiliation.getStopDate()));
			message = message.replace(EmailTemplateService.AFFILIATIONUUID_PLACEHOLDER, affiliation.getUuid());
			message = message.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, PersonService.getName(affiliation.getPerson()));
			message = message.replace(EmailTemplateService.VENDOR_PLACEHOLDER, (affiliation.getVendor() != null) ? affiliation.getVendor() : "");
			message = message.replace(EmailTemplateService.INTERNAL_REFERENCE_PLACEHOLDER, (affiliation.getInternalReference() != null) ? affiliation.getInternalReference() : "");
			message = message.replace(EmailTemplateService.DAYS_BEFORE_EVENT, "" + child.getDaysBeforeEvent());
			message = message.replace(EmailTemplateService.POSITION_NAME_PLACEHOLDER, AffiliationService.getPositionName(affiliation));
			message = message.replace(EmailTemplateService.EMPLOYEE_NUMBER_PLACEHOLDER, affiliation.getEmployeeId() != null ? affiliation.getEmployeeId() : "");
			message = message.replace(EmailTemplateService.ACCOUNT_PLACEHOLDER, userId);

			
			String title = child.getTitle();
			title = title.replace(EmailTemplateService.RECEIVER_PLACEHOLDER, manager.getName());
			title = title.replace(EmailTemplateService.ORGUNIT_PLACEHOLDER, affiliation.getOrgUnit().getName());
			title = title.replace(EmailTemplateService.TIMESTAMP_PLACEHOLDER, dateFormat.format(affiliation.getStopDate()));
			title = title.replace(EmailTemplateService.AFFILIATIONUUID_PLACEHOLDER, affiliation.getUuid());
			title = title.replace(EmailTemplateService.EMPLOYEE_PLACEHOLDER, PersonService.getName(affiliation.getPerson()));
			title = title.replace(EmailTemplateService.VENDOR_PLACEHOLDER, (affiliation.getVendor() != null) ? affiliation.getVendor() : "");
			title = title.replace(EmailTemplateService.INTERNAL_REFERENCE_PLACEHOLDER, (affiliation.getInternalReference() != null) ? affiliation.getInternalReference() : "");
			title = title.replace(EmailTemplateService.DAYS_BEFORE_EVENT, "" + child.getDaysBeforeEvent());
			title = title.replace(EmailTemplateService.POSITION_NAME_PLACEHOLDER, AffiliationService.getPositionName(affiliation));
			title = title.replace(EmailTemplateService.EMPLOYEE_NUMBER_PLACEHOLDER, affiliation.getEmployeeId() != null ? affiliation.getEmployeeId() : "");
			title = title.replace(EmailTemplateService.ACCOUNT_PLACEHOLDER, userId);

			for (String email : emailRecipients) {
				emailQueueService.queueEmail(email, title, message, 0, child);
			}
		}
	}

	private String getUserIdForAffiliation(Affiliation affiliation) {
		String userId = "";
		List<User> users = affiliation.getPerson().getUsers().stream().map(u -> u.getUser()).collect(Collectors.toList());
		Optional<User> user = users.stream().filter(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType()) && Objects.equals(u.getEmployeeId(), affiliation.getEmployeeId())).findAny();
		if (user.isPresent()) {
			userId = user.get().getUserId();
		} else {
			Optional<User> primeUser = users.stream().filter(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType()) && u.isPrime()).findAny();
			if (primeUser.isPresent()) {
				userId = primeUser.get().getUserId();
			}
		}
		return userId;
	}


	@Transactional
	public void sendResignationEmails() {
		
		// sanity check
		EmailTemplate resignationReminder = emailTemplateService.findByTemplateType(EmailTemplateType.RESIGNATION);
		EmailTemplate substituteReminder = emailTemplateService.findByTemplateType(EmailTemplateType.SUBSTITUTE_STOPS);
		
		boolean foundTemplate = false;
		for (EmailTemplateChild child : resignationReminder.getChildren()) {
			if (child.isEnabled()) {
				foundTemplate = true;
			}
		}
		
		for (EmailTemplateChild child : substituteReminder.getChildren()) {
			if (child.isEnabled()) {
				foundTemplate = true;
			}
		}

		if (!foundTemplate) {
			return;
		}

		List<Affiliation> affiliations = findByMaster(configuration.getModules().getLos().getPrimeAffiliationMaster());

		for (EmailTemplateChild child : resignationReminder.getChildren()) {
			if (!child.isEnabled()) {
				continue;
			}
			
			LocalDate xDaysFromNow = LocalDate.now().plusDays(child.getDaysBeforeEvent());
			List<Affiliation> affiliationsEndingInXDays = affiliations.stream().filter(a -> a.getStopDate() != null && toLocalDate(a.getStopDate()).equals(xDaysFromNow)).collect(Collectors.toList());

			processResignationTemplate(affiliationsEndingInXDays, child, resignationReminder);
		}

		List<SubstituteAssignment> substituteAssignments = substituteAssignmentService.findAll();
		for (EmailTemplateChild child : substituteReminder.getChildren()) {
			if (!child.isEnabled()) {
				continue;
			}

			LocalDate xDaysFromNow = LocalDate.now().plusDays(5);
			List<Affiliation> affiliationsEndingIn5Days = affiliations.stream().filter(a -> a.getStopDate() != null && toLocalDate(a.getStopDate()).equals(xDaysFromNow)).collect(Collectors.toList());
			
			if (substituteAssignments.size() > 0) {
				processSubstituteStopsTemplate(affiliationsEndingIn5Days, child, substituteAssignments);
			}
		}
	}
	
	// ensure we know when today and yesterday are ;)
	
	// runs one minute past midnight
	@Scheduled(cron = "1 0 0 * * ?")
	public void generateTodayAndYesterday() {
		_today = null;
		_yesterday = null;
	}

	private static Date _today = null;
	private static Date getToday() {
		if (_today == null) {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			cal.set(Calendar.HOUR_OF_DAY, 8);   // all stop_dates are set to 00:00, so by picking 08:00, we do not have a 00:00:00 vs 00:00:00 issue
			_today = cal.getTime();      // today at 08:00
		}
		
		return _today;
	}
	
	private static Date _yesterday = null;
	private static Date getYesterday() {
		if (_yesterday == null) {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			cal.set(Calendar.HOUR_OF_DAY, 8);      // all stop_dates are set to 00:00, so by picking 08:00, we do not have a 00:00:00 vs 00:00:00 issue
			cal.add(Calendar.DATE, -1);
			
			_yesterday = cal.getTime();     // yesterday at 08:00			
		}
		
		return _yesterday;
	}
	
	private static Date getMonthsAgo(int months) {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.HOUR_OF_DAY, 8);      // all stop_dates are set to 00:00, so by picking 08:00, we do not have a 00:00:00 vs 00:00:00 issue
		cal.add(Calendar.MONTH, (-1 * months));

		return cal.getTime();     // x months ago at 08:00
	}
}
