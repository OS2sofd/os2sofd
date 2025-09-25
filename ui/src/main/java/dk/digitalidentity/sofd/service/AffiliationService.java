package dk.digitalidentity.sofd.service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.AffiliationDao;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.EmailTemplate;
import dk.digitalidentity.sofd.dao.model.EmailTemplateChild;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SubstituteAssignment;
import dk.digitalidentity.sofd.dao.model.SubstituteContext;
import dk.digitalidentity.sofd.dao.model.SubstituteOrgUnitAssignment;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.dao.model.enums.EmailTemplatePlaceholder;
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
	private static SofdConfiguration _configuration;
	
	@Autowired
	private EmailTemplateService emailTemplateService;
	
	@Autowired
	private EmailQueueService emailQueueService;
	
	@Autowired
	private SubstituteAssignmentService substituteAssignmentService;

	@Autowired
	private SubstituteOrgUnitAssignmentService substituteOrgUnitAssignmentService;

	@Autowired
	private PersonService personService;
	
	@Autowired
	private EmailTemplateChildService emailTemplateChildService;

	// need static access to this field
	@EventListener(ApplicationReadyEvent.class)
	public void runOnStartup() {
		AffiliationService._configuration = this.configuration;
	}

	public List<Affiliation> findAll() {
		return affiliationDao.findAll();
	}

	public List<Affiliation> findByMaster(String master) {
		return affiliationDao.findByMaster(master);
	}

	public Affiliation findById(long affiliationId) {
		return affiliationDao.findById(affiliationId);
	}
	
	// NO - please use findById() if possible - this does not have index (and is not primary key)
	public Affiliation findByUuid(String uuid) {
		return affiliationDao.findByUuid(uuid);
	}

	@Deprecated (forRemoval = true)
	public Affiliation save(Affiliation affiliation) throws NotImplementedException {
		throw new NotImplementedException("NO - don't update affiliation directly - update the person instead!");
	}

	public Long countByOrgUnitAndActive(OrgUnit orgUnit) {
		return affiliationDao.countByOrgUnitAndActive(orgUnit.getUuid());
	}

	public Long countByOrgUnitAndActiveRecursive(OrgUnit orgUnit) {
		return affiliationDao.countByOrgUnitAndActiveRecursive(orgUnit.getUuid());
	}

	public List<Affiliation> findByCalculatedOrgUnitAndActive(OrgUnit orgUnit) {
		return affiliationDao.findByCalculatedOrgUnitAndActive(orgUnit.getUuid());
	}
	public List<Affiliation> findByOrgUnit(OrgUnit orgUnit) {
		return affiliationDao.findByOrgUnit(orgUnit);
	}

	public List<Affiliation> findByCalculatedOrgUnit(OrgUnit orgUnit) {
		return affiliationDao.findByCalculatedOrgUnit(orgUnit.getUuid());
	}

	@Deprecated (forRemoval = true)
	public void delete(Affiliation affiliation) throws NotImplementedException {
		throw new NotImplementedException("NO - don't update affiliation directly - update the person instead!");
	}
	
	public static List<String> getFunctions(Affiliation affiliation) {
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

	public static String getPositionName(Affiliation affiliation) {
		if (affiliation.getPositionDisplayName() != null && affiliation.getPositionDisplayName().length() > 0) {
			return affiliation.getPositionDisplayName();
		}
		
		if (_configuration.getModules().getProfessions().isEnableOutgoing()) {
			if (affiliation.getProfession() != null) {
				return affiliation.getProfession().getName();
			}
		}

		return affiliation.getPositionName();
	}

	// helper methods to decide if an affiliation is active
	
	public static List<Affiliation> onlyActiveAffiliations(List<Affiliation> affiliations) {
		return onlyActiveAffiliations(affiliations, 0);
	}

	// includes future affiliations
	public static List<Affiliation> notStoppedAffiliations(List<Affiliation> affiliations) {
		return CollectionUtils.emptyIfNull(affiliations).stream()
				.filter(a ->
						(!a.isDeleted()) && // TODO: goes away at some point
						(!notActiveAnymore(a)))
				.collect(Collectors.toList());
	}

	// only includes affiliations that are active right now
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
	
	private void processSubstituteStopsTemplate(List<Affiliation> affiliations, EmailTemplateChild child, List<SubstituteAssignment> substituteAssignments, List<SubstituteOrgUnitAssignment> substituteOrgUnitAssignments) {
		for (Affiliation affiliation : affiliations) {
			List<SubstituteAssignment> subs = substituteAssignments.stream()
					.filter(a -> Objects.equals(a.getSubstitute().getUuid(), affiliation.getPerson().getUuid()))
					.collect(Collectors.toList());

			List<SubstituteOrgUnitAssignment> substituteOrgUnitAssignmentsForAffiliation = substituteOrgUnitAssignments.stream()
					.filter(a -> Objects.equals(a.getSubstitute().getUuid(), affiliation.getPerson().getUuid()))
					.collect(Collectors.toList());

			for (SubstituteAssignment substituteAssignment : subs) {
				queueEmail(child, substituteAssignment.getPerson(), substituteAssignment.getSubstitute(), substituteAssignment.getContext(), affiliation.getCalculatedOrgUnit());
			}

			for (SubstituteOrgUnitAssignment substituteOrgUnitAssignment : substituteOrgUnitAssignmentsForAffiliation) {
				if( substituteOrgUnitAssignment.getOrgUnit().getManager() != null ) {
					queueEmail(child, substituteOrgUnitAssignment.getOrgUnit().getManager().getManager(), substituteOrgUnitAssignment.getSubstitute(), substituteOrgUnitAssignment.getContext(), substituteOrgUnitAssignment.getOrgUnit());
				}
			}
		}
	}

	private void queueEmail(EmailTemplateChild child, Person manager, Person substitute, SubstituteContext context, OrgUnit orgUnit) {
		String email = PersonService.getEmail(manager);
		if (!StringUtils.hasLength(email)) {
			log.warn("processSubstituteStopsTemplate - no email address found.");
			return;
		}

		String message = child.getMessage();
		message = message.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), PersonService.getName(manager));
		message = message.replace(EmailTemplatePlaceholder.SUBSTITUTE_PLACEHOLDER.getPlaceholder(), PersonService.getName(substitute));
		message = message.replace(EmailTemplatePlaceholder.SUBSTITUTE_CONTEXT_PLACEHOLDER.getPlaceholder(), context.getName());
		message = message.replace(EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER.getPlaceholder(), orgUnit.getName());

		String title = child.getTitle();
		title = title.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), PersonService.getName(manager));
		title = title.replace(EmailTemplatePlaceholder.SUBSTITUTE_PLACEHOLDER.getPlaceholder(), PersonService.getName(substitute));
		title = title.replace(EmailTemplatePlaceholder.SUBSTITUTE_CONTEXT_PLACEHOLDER.getPlaceholder(), context.getName());
		title = title.replace(EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER.getPlaceholder(), orgUnit.getName());

		var logContext = new StringBuilder();
		logContext.append("Skabelon: ").append(child.getTitle());
		logContext.append(", ").append("Stedfortræder: ").append(PersonService.getName(substitute));
		logContext.append(", ").append("Stedfortræderkontekst: ").append(context.getName());
		logContext.append(", ").append("Enhed: ").append(orgUnit.getName());

		emailQueueService.queueEmail(manager, title, message, 0, child, logContext.toString());
	}

	private void processResignationTemplate(List<Affiliation> affiliations, EmailTemplateChild child, EmailTemplate template) {
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

		for (Affiliation affiliation : affiliations) {
			var managerResponse = PersonService.getManagerDifferentFromPerson(affiliation.getPerson(), affiliation);
			if (managerResponse == null) {
				log.warn("OrgUnit " + affiliation.getCalculatedOrgUnit().getName() + " does not have a manager");
				continue;
			}

			if( !emailTemplateService.shouldIncludeOrgUnit(child,affiliation.getCalculatedOrgUnit().getUuid()) ) {
				log.debug("Not sending email for email template child with id " + child.getId() + " for affiliation with uuid " + (affiliation != null ? affiliation.getUuid() : "<null>") + ". The affiliation OU was filtered out.");
				continue;
			}

			if (configuration.getEmailTemplate().isAdUserFilterEnabled() && template.getTemplateType().isShowADUserFilter() && child.isAdRequired()) {
				Person person = affiliation.getPerson();
				List<User> adUsers = person.getUsers().stream().map(m -> m.getUser()).filter(u -> !u.isDisabled() && SupportedUserTypeService.isActiveDirectory(u.getUserType())).collect(Collectors.toList());
				long matchCount = adUsers.stream().filter(u -> (u.getEmployeeId() != null && u.getEmployeeId().equals(affiliation.getEmployeeId())) || u.getEmployeeId() == null ).count();
				
				if (matchCount == 0) {
					continue;
				}
			}

			
			String userId = getUserIdForAffiliation( affiliation);

			String message = child.getMessage();
			message = message.replace(EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER.getPlaceholder(), affiliation.getCalculatedOrgUnit().getName());
			message = message.replace(EmailTemplatePlaceholder.TIMESTAMP_PLACEHOLDER.getPlaceholder(), dateFormat.format(affiliation.getStopDate()));
			message = message.replace(EmailTemplatePlaceholder.AFFILIATIONUUID_PLACEHOLDER.getPlaceholder(), affiliation.getUuid());
			message = message.replace(EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER.getPlaceholder(), PersonService.getName(affiliation.getPerson()));
			message = message.replace(EmailTemplatePlaceholder.VENDOR_PLACEHOLDER.getPlaceholder(), (affiliation.getVendor() != null) ? affiliation.getVendor() : "");
			message = message.replace(EmailTemplatePlaceholder.INTERNAL_REFERENCE_PLACEHOLDER.getPlaceholder(), (affiliation.getInternalReference() != null) ? affiliation.getInternalReference() : "");
			message = message.replace(EmailTemplatePlaceholder.DAYS_BEFORE_EVENT.getPlaceholder(), "" + child.getDaysBeforeEvent());
			message = message.replace(EmailTemplatePlaceholder.POSITION_NAME_PLACEHOLDER.getPlaceholder(), AffiliationService.getPositionName(affiliation));
			message = message.replace(EmailTemplatePlaceholder.EMPLOYEE_NUMBER_PLACEHOLDER.getPlaceholder(), affiliation.getEmployeeId() != null ? affiliation.getEmployeeId() : "");
			message = message.replace(EmailTemplatePlaceholder.ACCOUNT_PLACEHOLDER.getPlaceholder(), userId);

			String title = child.getTitle();
			title = title.replace(EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER.getPlaceholder(), affiliation.getCalculatedOrgUnit().getName());
			title = title.replace(EmailTemplatePlaceholder.TIMESTAMP_PLACEHOLDER.getPlaceholder(), dateFormat.format(affiliation.getStopDate()));
			title = title.replace(EmailTemplatePlaceholder.AFFILIATIONUUID_PLACEHOLDER.getPlaceholder(), affiliation.getUuid());
			title = title.replace(EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER.getPlaceholder(), PersonService.getName(affiliation.getPerson()));
			title = title.replace(EmailTemplatePlaceholder.VENDOR_PLACEHOLDER.getPlaceholder(), (affiliation.getVendor() != null) ? affiliation.getVendor() : "");
			title = title.replace(EmailTemplatePlaceholder.INTERNAL_REFERENCE_PLACEHOLDER.getPlaceholder(), (affiliation.getInternalReference() != null) ? affiliation.getInternalReference() : "");
			title = title.replace(EmailTemplatePlaceholder.DAYS_BEFORE_EVENT.getPlaceholder(), "" + child.getDaysBeforeEvent());
			title = title.replace(EmailTemplatePlaceholder.POSITION_NAME_PLACEHOLDER.getPlaceholder(), AffiliationService.getPositionName(affiliation));
			title = title.replace(EmailTemplatePlaceholder.EMPLOYEE_NUMBER_PLACEHOLDER.getPlaceholder(), affiliation.getEmployeeId() != null ? affiliation.getEmployeeId() : "");
			title = title.replace(EmailTemplatePlaceholder.ACCOUNT_PLACEHOLDER.getPlaceholder(), userId);

			var logContext = new StringBuilder();
			logContext.append("Skabelon: ").append(child.getTitle());
			logContext.append(", ").append("Medarbejder: ").append(PersonService.getName(affiliation.getPerson()));
			logContext.append(", ").append("Medarbejdernummer: ").append(affiliation.getEmployeeId());
			logContext.append(", ").append("Enhed: ").append(affiliation.getCalculatedOrgUnit().getName());

			List<String> manualRecipients = emailTemplateChildService.getRecipientsList(child.getRecipients());
			for( var recipient : manualRecipients ) {
				var recipientMessage = message.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), recipient);
				var recipientTitle = title = title.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), recipient);
				emailQueueService.queueEmail(recipient, recipientTitle, recipientMessage, 0, child, logContext.toString());
			}

			if (!child.isOnlyManualRecipients()) {
				List<Person> recipients = emailTemplateService.getManagerOrSubstitutes(child, managerResponse.getManager(), affiliation.getCalculatedOrgUnit().getUuid());

				if (CollectionUtils.isEmpty(recipients)) {
					log.warn("ProcessAffiliation - no email address found.");
					continue;
				}

				message = message.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), managerResponse.getName());
				title = title.replace(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER.getPlaceholder(), managerResponse.getName());
				for (Person recipient : recipients) {
					emailQueueService.queueEmail(recipient, title, message, 0, child, logContext.toString());
				}
			}
		}
	}

	private String getUserIdForAffiliation(Affiliation affiliation) {
		String userId = "";
		List<User> users = affiliation.getPerson().getUsers().stream().map(u -> u.getUser()).collect(Collectors.toList());
		// first try to find a user that is specifically mapped to given affiliation
		Optional<User> user = users.stream().filter(u -> SupportedUserTypeService.isActiveDirectory(u.getUserType()) && Objects.equals(u.getEmployeeId(), affiliation.getEmployeeId())).findAny();
		if (user.isPresent()) {
			userId = user.get().getUserId();
		} else {
			// then try to find a user that is prime, but not mapped to a specific affiliation
			Optional<User> primeUser = users.stream().filter(u -> !StringUtils.hasLength(u.getEmployeeId()) && SupportedUserTypeService.isActiveDirectory(u.getUserType()) && u.isPrime()).findAny();
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
		List<SubstituteOrgUnitAssignment> substituteOrgUnitAssignments = substituteOrgUnitAssignmentService.getAll();
		for (EmailTemplateChild child : substituteReminder.getChildren()) {
			if (!child.isEnabled()) {
				continue;
			}

			LocalDate xDaysFromNow = LocalDate.now().plusDays(child.getDaysBeforeEvent());
			List<Affiliation> affiliationsEndingIn5Days = affiliations.stream().filter(a -> a.getStopDate() != null && toLocalDate(a.getStopDate()).equals(xDaysFromNow)).collect(Collectors.toList());
			
			if (substituteAssignments.size() > 0) {
				processSubstituteStopsTemplate(affiliationsEndingIn5Days, child, substituteAssignments, substituteOrgUnitAssignments);
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
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			cal.set(Calendar.HOUR_OF_DAY, 23);      // all stop_dates are set to 23:50, so by picking 23:59, we do not have a 00:00:00 vs 00:00:00 issue

			_today = cal.getTime();      // today at 23:59
		}
		
		return _today;
	}
	
	private static Date _yesterday = null;
	private static Date getYesterday() {
		if (_yesterday == null) {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.MINUTE, 59);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.MILLISECOND, 0);
			cal.set(Calendar.HOUR_OF_DAY, 23);      // all stop_dates are set to 23:50, so by picking 23:59, we do not have a 00:00:00 vs 00:00:00 issue
			cal.add(Calendar.DATE, -1);
			
			_yesterday = cal.getTime();     // yesterday at 23:59
		}
		
		return _yesterday;
	}
	
	private static Date getMonthsAgo(int months) {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.HOUR_OF_DAY, 23);      // all stop_dates are set to 23:50, so by picking 23:59, we do not have a 00:00:00 vs 00:00:00 issue
		cal.add(Calendar.MONTH, (-1 * months));

		return cal.getTime();     // x months ago at 23:59
	}

	private static Date getDaysAgo(int days) {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.MINUTE, 59);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.HOUR_OF_DAY, 23);      // all stop_dates are set to 23:50, so by picking 23:59, we do not have a 00:00:00 vs 00:00:00 issue
		cal.add(Calendar.DAY_OF_MONTH, (-1 * days));

		return cal.getTime();     // x months ago at 23:59
	}

	/**
	 * Deletes all affiliations where startDate = stopDate
	 */
	@Transactional
	public void deleteInvalidOpusAffiliations() {
		affiliationDao.deleteInvalidOpusAffiliations();
	}

	public List<Affiliation> getByEmployeeId(String employeeId) {
		return affiliationDao.findByEmployeeId(employeeId);
	}

	@Transactional
	public void deleteOldAffiliations() {
		int days = configuration.getScheduled().getDeleteOldAffiliations().getDays();
		affiliationDao.deleteByMasterAndStopDateNotNullAndStopDateBefore("SOFD", getDaysAgo(days));
	}

	@Transactional
	public void setNewlyActiveAffiliationsPrime() {
		for (Affiliation affiliation : getActiveAffiliationsThatShouldBePrime()) {
			Person person = affiliation.getPerson();
			for (Affiliation affiliationFromPerson : person.getAffiliations()) {
				if (affiliation.getId() == affiliationFromPerson.getId()) {
					affiliation.setSelectedPrime(true);
					affiliation.setUseAsPrimaryWhenActive(false);
				} else {
					affiliationFromPerson.setSelectedPrime(false);
				}
			}

			personService.save(person);
		}
	}

	private List<Affiliation> getActiveAffiliationsThatShouldBePrime() {
		List<Affiliation> affiliations = affiliationDao.findByUseAsPrimaryWhenActiveTrue();
		return onlyActiveAffiliations(affiliations);
	}

	public Set<String> getPersonUuidsWithAffiliationHistory(List<String> masters) {
		return affiliationDao.getPersonUuidsWithAffiliationHistory(masters);
	}
}
