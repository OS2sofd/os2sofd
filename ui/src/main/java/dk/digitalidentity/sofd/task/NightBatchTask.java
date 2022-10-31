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

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.dao.model.BatchJobExecution;
import dk.digitalidentity.sofd.service.AccountOrderService;
import dk.digitalidentity.sofd.service.AffiliationService;
import dk.digitalidentity.sofd.service.BatchJobExecutionService;
import dk.digitalidentity.sofd.service.KleService;
import dk.digitalidentity.sofd.service.OS2SyncService;
import dk.digitalidentity.sofd.service.PersonService;
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
	private UserChangeEmployeeIdQueueService userChangeEmployeeIdQueueService;

	@Autowired
	private BatchJobExecutionService batchJobExecutionService;
	
	@PostConstruct
	public void init() {
		if (!configuration.getScheduled().isEnabled()) {
			log.debug("Scheduled jobs are disabled on this instance");
			return;
		}
		
		log.info("Generating nightbatch task schedule");

		// clean old account orders (02:10 - 02:20)
		batchJobs.add(BatchJob.builder()
				.name("Account Order Cleanup Task")
				.time(LocalTime.of(2, random.nextInt(10) + 10))
				.function(() -> {
					accountOrderService.cleanupOld();

					return true;
				}).build());

		// Update AD users' future employee ids
		batchJobs.add(BatchJob.builder()
				.name("Update AD Future EmployeeID Task")
				.time(LocalTime.of(2, random.nextInt(55)))
				.function(() -> {
					userChangeEmployeeIdQueueService.handleChanges();

					return true;
				}).build());

		// generate new account orders (03:15 - 03:45)
		if (configuration.getModules().getAccountCreation().isEnabled()) {
			batchJobs.add(BatchJob.builder()
					.name("Generate Account Orders Task")
					.time(LocalTime.of(3, random.nextInt(30) + 15))
					.function(() -> {
						accountOrderService.nightlyJob();

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

		// load KLE from external source every Saturday morning (09:00 to 09:55)
		batchJobs.add(BatchJob.builder()
				.name("Load KLE Task")
				.time(LocalTime.of(9, random.nextInt(55)))
				.dayOfWeek(DayOfWeek.SATURDAY)
				.function(() -> {
					kleService.updateCache();

					return true;
				}).build());

		// refresh KLE from database every Saturday morning (10:15 to 10:30)
		batchJobs.add(BatchJob.builder()
				.name("Refresh KLE Task")
				.time(LocalTime.of(10, random.nextInt(15) + 15))
				.dayOfWeek(DayOfWeek.SATURDAY)
				.function(() -> {
					kleService.reloadCache();

					return true;
				}).build());

		// cleanup orgUnits in FK Organisation every Monday morning (10:00 to 10:59)
		batchJobs.add(BatchJob.builder()
				.name("Cleanup OUs in FK Organisation")
				.time(LocalTime.of(10, random.nextInt(59)))
				.dayOfWeek(DayOfWeek.MONDAY)
				.function(() -> {
					os2SyncService.synchronizeHierarchy();

					return true;
				}).build());

		// update last execution time from database
		List<BatchJobExecution> batchJobExecutionEntires = batchJobExecutionService.findAll();
		
		for (BatchJob batchJob : batchJobs) {
			BatchJobExecution batchJobExecutionEntry = batchJobExecutionEntires.stream().filter(e -> Objects.equals(e.getJobName(), batchJob.getName())).findAny().orElse(null);

			if (batchJobExecutionEntry != null) {
				batchJob.setLastExecutionTime(batchJobExecutionEntry.getLastExecutionTime());
			}
			else {
				batchJobExecutionEntry = new BatchJobExecution();
				batchJobExecutionEntry.setJobName(batchJob.getName());
				batchJobExecutionEntry.setLastExecutionTime(null);
				
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
				try {
					boolean result = batchJob.getFunction().get();
					if (result != true) {
						throw new Exception("Batchjob failed: " + batchJob.getName());
					}
					batchJob.setLastExecutionTime(new Date());
					batchJobExecutionService.updateExecutionTime(batchJob.getName(), batchJob.getLastExecutionTime());
				}
				catch (Exception ex) {
					log.error("Failed to execute: " + batchJob.getName(), ex);
				}
			}
		}
	}
}
