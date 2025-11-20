package dk.digitalidentity.sofd.task;

import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.BatchJobExecution;
import dk.digitalidentity.sofd.service.AccountOrderService;
import dk.digitalidentity.sofd.service.AffiliationService;
import dk.digitalidentity.sofd.service.AuthorizationCodeService;
import dk.digitalidentity.sofd.service.BatchJobExecutionService;
import dk.digitalidentity.sofd.service.CvrService;
import dk.digitalidentity.sofd.service.FunctionAssignmentService;
import dk.digitalidentity.sofd.service.KleService;
import dk.digitalidentity.sofd.service.KnownUsernamesService;
import dk.digitalidentity.sofd.service.ManagerService;
import dk.digitalidentity.sofd.service.ModificationHistoryService;
import dk.digitalidentity.sofd.service.NotificationService;
import dk.digitalidentity.sofd.service.OS2SyncService;
import dk.digitalidentity.sofd.service.OrgUnitFutureChangesService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.SubstituteAssignmentService;
import dk.digitalidentity.sofd.service.SubstituteOrgUnitAssignmentService;
import dk.digitalidentity.sofd.service.UserChangeEmployeeIdQueueService;
import dk.digitalidentity.sofd.task.model.BatchJob;
import lombok.extern.slf4j.Slf4j;

@Component
@EnableScheduling
@Slf4j
public class NightBatchTask {
	private SecureRandom random = new SecureRandom();
	private List<BatchJob> batchJobs = new ArrayList<>();

	@Autowired
	private SofdConfiguration configuration;
	
	@Autowired
	private AccountOrderService accountOrderService;
	
	@Autowired
	private PersonService personService;

	@Autowired
	private AffiliationService affiliationService;
	
	@Autowired
	private KleService kleService;
	
	@Autowired
	private OS2SyncService os2SyncService;
	
	@Autowired
	private CvrService cvrService;
	
	@Autowired
	private NotificationService notificationService;
	
	@Autowired
	private UserChangeEmployeeIdQueueService userChangeEmployeeIdQueueService;

	@Autowired
	private OrgUnitFutureChangesService orgUnitFutureChangesService;
	
	@Autowired 
	private FunctionAssignmentService functionAssignmentService;
	
	@Autowired
	private KnownUsernamesService knownUsernamesService;
	
	@Autowired
	private ModificationHistoryService modificationHistoryService;

	@Autowired
	private AuthorizationCodeService authorizationCodeService;

	@Autowired
	private BatchJobExecutionService batchJobExecutionService;

	@Autowired
	private SubstituteAssignmentService substituteAssignmentService;

	@Autowired
	private SubstituteOrgUnitAssignmentService substituteOrgUnitAssignmentService;

	@Autowired
	private ManagerService managerService;

	@EventListener(ApplicationReadyEvent.class)
	public void init() {
		if (!configuration.getScheduled().isEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}

		if (configuration.getScheduled().isGenerateAccountOrdersOnStartupEnabled()) {
			log.info("Executing nightlyjob on startup");
			accountOrderService.nightlyJob();
		}

		log.info("Generating nightbatch task schedule");

		// sync auth codes (06:30 - 09:59)
		batchJobs.add(BatchJob.builder()
				.name("Authorization Codes Task")
				.time(LocalTime.of(random.nextInt(4) + 6, random.nextInt(30) + 30))
				.function(() -> {
					authorizationCodeService.syncAll(false);

					return true;
				}).build());

		// update from CVR (01:00 - 01:30)
		batchJobs.add(BatchJob.builder()
				.name("Known usernames Task")
				.time(LocalTime.of(1, random.nextInt(30)))
				.function(() -> {
					knownUsernamesService.findNewUsernames();

					return true;
				}).build());
		
		// update from CVR (01:00 - 01:30)
		if (configuration.getIntegrations().getCvr().isEnabled()) {
			batchJobs.add(BatchJob.builder()
					.name("CVR Maintenance Task")
					.time(LocalTime.of(1, random.nextInt(30)))
					.function(() -> {
						cvrService.cvrMaintenance();

						return true;
					}).build());
		}

		// update from CVR (01:30 - 01:59)
		batchJobs.add(BatchJob.builder()
				.name("Modification History Cleanup Task")
				.time(LocalTime.of(1, 30 + random.nextInt(30)))
				.function(() -> {
					modificationHistoryService.removeModificationHistoryOlderThan(configuration.getScheduled().getModificationHistoryCleanup().getDays());

					return true;
				}).build());

		// update from CVR (01:30 - 01:59)
		batchJobs.add(BatchJob.builder()
				.name("Person Leave Cleanup Task")
				.time(LocalTime.of(1, 30 + random.nextInt(30)))
				.function(() -> {
					personService.handlePersonsOnLeave();

					return true;
				}).build());
		
		// clean old account orders (02:10 - 02:20)
		batchJobs.add(BatchJob.builder()
				.name("Account Order Cleanup Task")
				.time(LocalTime.of(2, random.nextInt(10) + 10))
				.function(() -> {
					accountOrderService.cleanupOld();

					return true;
				}).build());

		// clean old AD data (02:00 - 02:30)
		batchJobs.add(BatchJob.builder()
				.name("Clean old AD data Task")
				.time(LocalTime.of(2, random.nextInt(30)))
				.function(() -> {
					personService.cleanupOldActiveDirectoryData();

					return true;
				}).build());

		// Update AD users' future employee ids (02:00 - 02:55)
		batchJobs.add(BatchJob.builder()
				.name("Update AD Future EmployeeID Task")
				.time(LocalTime.of(2, random.nextInt(55)))
				.function(() -> {
					userChangeEmployeeIdQueueService.handleChanges();

					return true;
				}).build());

		// apply future orgUnitChanges (03:00)
		if (configuration.getModules().getLos().isEnabled() && configuration.getModules().getLos().isFutureOrgsEnabled()) {
			batchJobs.add(BatchJob.builder()
					.name("Apply Future Orgunit Changes Task")
					.time(LocalTime.of(3, 0))
					.function(() -> {
						orgUnitFutureChangesService.mergeFutureChanges();

						return true;
					}).build());
		}

		// generate new account orders (05:15 - 05:45)
		if (configuration.getModules().getAccountCreation().isEnabled()) {
			batchJobs.add(BatchJob.builder()
					.name("Generate Account Orders Task")
					.time(LocalTime.of(5, random.nextInt(30) + 15))
					.function(() -> {
						accountOrderService.nightlyJob();

						return true;
					}).build());
		}

		// expire notifications (05:15 - 05:45)
		batchJobs.add(BatchJob.builder()
				.name("Notification Expire Task")
				.time(LocalTime.of(5, random.nextInt(30) + 15))
				.function(() -> {
					notificationService.expire();

					return true;
				}).build());

		// fh functions (07:15 - 07:45)
		if (configuration.getModules().getFunctionHierarchy().isEnabled()) {
			batchJobs.add(BatchJob.builder()
					.name("Function hierarchy task")
					.time(LocalTime.of(7, random.nextInt(30) + 15))
					.function(() -> {
						functionAssignmentService.generateFunctionAssignmentExpiringNotifications();
						functionAssignmentService.generateFunctionAssignmentFollowUpNotifications();
						return true;
					}).build());
		}

		// generate reminders about expiring SOFD affiliations (08:15 - 08:45)
		batchJobs.add(BatchJob.builder()
				.name("Affiliation Expiry Reminder Task (SOFD)")
				.time(LocalTime.of(8, random.nextInt(30) + 15))
				.function(() -> {
					personService.expiryReminder();

					return true;
				}).build());

		// generate reminders about expiring WAGES affiliations (08:15 - 08:45)
		batchJobs.add(BatchJob.builder()
				.name("Affiliation Expiry Reminder Task (WAGES)")
				.time(LocalTime.of(8, random.nextInt(30) + 15))
				.function(() -> {
					affiliationService.sendResignationEmails();

					return true;
				}).build());

		// generate reminders about expiring WAGES affiliations (05:15 - 05:45)
		batchJobs.add(BatchJob.builder()
				.name("Ensure Valid Managers Task")
				.time(LocalTime.of(5, random.nextInt(30) + 15))
				.function(() -> {
					managerService.ensureValidManagers();

					return true;
				}).build());


		// load KLE from external source every Saturday morning (09:00 to 09:55)
		batchJobs.add(BatchJob.builder()
				.name("Load KLE Task")
				.time(LocalTime.of(9, random.nextInt(55)))
				.dayOfWeek(DayOfWeek.SATURDAY)
				.function(() -> {
					kleService.updateCache();

					return true;
				}).build());

		// cleanup deleted persons every Saturday (09:00 to 09:55)
		batchJobs.add(BatchJob.builder()
				.name("Cleanup deleted persons Task")
				.time(LocalTime.of(9, random.nextInt(55)))
				.dayOfWeek(DayOfWeek.SATURDAY)
				.function(() -> {
					personService.cleanupDeletedPersons();

					return true;
				}).build());

		// refresh KLE from database every Saturday morning (10:15 to 10:30)
		batchJobs.add(BatchJob.builder()
				.name("Refresh KLE Task")
				.time(LocalTime.of(10, random.nextInt(15) + 15))
				.dayOfWeek(DayOfWeek.SATURDAY)
				.function(() -> {
					kleService.reloadCache(false);

					return true;
				}).build());

		// cleanup person flags (10:15 to 10:45)
		if (configuration.getScheduled().getFlagCleanup().isEnabled()) {
			batchJobs.add(BatchJob.builder()
					.name("Cleanup Person Flags Task")
					.time(LocalTime.of(10, random.nextInt(30) + 15))
					.function(() -> {
						personService.cleanupDeletedFlag();
	
						return true;
					}).build());
		}

		// delete substitutes with no affiliations (10:15 to 10:45)
		if (configuration.getScheduled().getDeleteSubstitutes().isEnabled()) {
			batchJobs.add(BatchJob.builder()
					.name("Delete Substitutes Task")
					.time(LocalTime.of(10, random.nextInt(30) + 15))
					.function(() -> {
						substituteAssignmentService.Cleanup(configuration.getScheduled().getDeleteSubstitutes().getDays());
						substituteOrgUnitAssignmentService.Cleanup(configuration.getScheduled().getDeleteSubstitutes().getDays());

						return true;
					}).build());
		}

		
		// cleanup orgUnits in FK Organisation every Monday (10:00 to 15:59)
		batchJobs.add(BatchJob.builder()
				.name("Cleanup OUs in FK Organisation")
				.time(LocalTime.of(10 + random.nextInt(6), random.nextInt(59)))
				.dayOfWeek(DayOfWeek.MONDAY)
				.function(() -> {
					os2SyncService.synchronizeHierarchy();

					return true;
				}).build());
		
		// cleanup users in FK Organisation every Tuesday (10:00 to 15:59)
		batchJobs.add(BatchJob.builder()
				.name("Cleanup users in FK Organisation")
				.time(LocalTime.of(10 + random.nextInt(6), random.nextInt(59)))
				.dayOfWeek(DayOfWeek.TUESDAY)
				.function(() -> {
					os2SyncService.cleanupUsers();
					return true;
				}).build());

		// update last execution time and error count from database
		List<BatchJobExecution> batchJobExecutionEntires = batchJobExecutionService.findAll();
		
		for (BatchJob batchJob : batchJobs) {
			BatchJobExecution batchJobExecutionEntry = batchJobExecutionEntires.stream().filter(e -> Objects.equals(e.getJobName(), batchJob.getName())).findAny().orElse(null);

			if (batchJobExecutionEntry != null) {
				batchJob.setLastExecutionTime(batchJobExecutionEntry.getLastExecutionTime());
				batchJob.setLastErrorTime(batchJobExecutionEntry.getLastErrorTime());
				batchJob.setErrorCount(batchJobExecutionEntry.getErrorCount());
			}
			else {
				batchJobExecutionEntry = new BatchJobExecution();
				batchJobExecutionEntry.setJobName(batchJob.getName());
				batchJobExecutionEntry.setLastExecutionTime(null);
				batchJobExecutionEntry.setErrorCount(0);
				batchJobExecutionService.save(batchJobExecutionEntry);
			}
		}
		
		Collections.sort(batchJobs, Comparator.comparing(BatchJob::getTime));

		StringBuilder builder = new StringBuilder();
		builder.append("Scheduling batchhjobs:\n");
		for (BatchJob batchJob : batchJobs) {
			builder.append(batchJob.toString());
		}

		log.info(builder.toString());
	}

	// all jobs should be scheduled to run between 00:00 and 11:59
	@Scheduled(cron = "0 * 0-11 * * ?")
	public void exectuteBatchJobs() {

		for (BatchJob batchJob : batchJobs) {
			if (batchJob.shouldRun()) {
				log.info("Executing batchjob: " + batchJob.getName());
				var errorCount = batchJobExecutionService.getErrorCount(batchJob.getName());				
				try {
					boolean result = batchJob.getFunction().get();
					if (result != true) {
						throw new Exception("Batchjob failed: " + batchJob.getName());
					}
					batchJob.setLastExecutionTime(new Date());
					batchJobExecutionService.updateExecutionTime(batchJob.getName(), batchJob.getLastExecutionTime());
					
					log.info("Finished executing batchjob: " + batchJob.getName());
				}
				catch (Exception ex) {
					errorCount++;
					batchJob.setErrorCount(errorCount);
					batchJob.setLastErrorTime(new Date());
					batchJobExecutionService.setErrorCount(batchJob.getName(),batchJob.getErrorCount(),batchJob.getLastErrorTime());
					var message = "Failed to execute: " + batchJob.getName() + " ("+ errorCount + " of " + batchJob.getMaxExecutionAttempts() + " attempts)";
					if( batchJob.getErrorCount() < batchJob.getMaxExecutionAttempts()) {
						log.warn(message,ex);
					}
					else {
						log.error(message, ex);
					}					
				}
			}
		}
	}
}
