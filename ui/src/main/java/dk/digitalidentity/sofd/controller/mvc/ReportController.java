package dk.digitalidentity.sofd.controller.mvc;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;

import dk.digitalidentity.sofd.config.SessionConstants;
import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.dto.AccountOrderDTO;
import dk.digitalidentity.sofd.controller.mvc.xls.GenericReportXlsView;
import dk.digitalidentity.sofd.controller.mvc.xls.MultipleAffiliationsReportXlsView;
import dk.digitalidentity.sofd.controller.mvc.xls.SofdAffiliationsReportXlsView;
import dk.digitalidentity.sofd.dao.model.AccountOrder;
import dk.digitalidentity.sofd.dao.model.Notification;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderStatus;
import dk.digitalidentity.sofd.dao.model.enums.ReportType;
import dk.digitalidentity.sofd.security.RequireControllerWriteAccess;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.service.AccountOrderService;
import dk.digitalidentity.sofd.service.NotificationService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.ReportService;
import dk.digitalidentity.sofd.service.S3Service;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequireReadAccess
@Controller
public class ReportController {

	@Autowired
	private AccountOrderService accountOrderService;

	@Autowired
	private PersonService personService;

	@Autowired
	private NotificationService adminTasksService;

	@Autowired
	private MessageSource messageSource;

	@Autowired
	private SofdConfiguration configuration;

	@Autowired
	private S3Service s3Service;

	@Autowired
	private ReportService reportService;
	
	@GetMapping("/ui/report/accountorders")
	public String accountOrders(Model model) {
		List<AccountOrder> adAccountOrders = accountOrderService.findAll();
		adAccountOrders.sort(Comparator.comparing(AccountOrder::getStatus));

		List<AccountOrderDTO> dtos = new ArrayList<>();
		for (AccountOrder order : adAccountOrders) {
			Person person = personService.getByUuid(order.getPersonUuid());
			if (person == null) {
				log.warn("Could not find person with uuid: " + order.getPersonUuid());
				continue;
			}
			
			dtos.add(new AccountOrderDTO(order, person));
		}
		
		model.addAttribute("orders", dtos);
		
		return "report/accountorders";
	}
	
	@GetMapping("/ui/report/accountorders/{id}")
	public String accountOrder(Model model, @PathVariable("id") long id) {
		AccountOrder order = accountOrderService.findById(id);
		if (order == null) {
			return "redirect:/ui/report/accountorders";
		}

		Person person = personService.getByUuid(order.getPersonUuid());
		if (person == null) {
			log.warn("Could not find person with uuid: " + order.getPersonUuid());
			
			return "redirect:/ui/report/accountorders";
		}

		AccountOrderDTO orderDTO = new AccountOrderDTO(order, person);
		
		model.addAttribute("order", orderDTO);
		
		return "report/accountorderView";
	}
	
	@RequireControllerWriteAccess
	@GetMapping("/ui/report/accountorders/delete/{id}")
	public String deleteAccountOrder(Model model, @PathVariable("id") long id) {
		AccountOrder order = accountOrderService.findById(id);
		if (order != null && order.getStatus().equals(AccountOrderStatus.FAILED)) {
			accountOrderService.delete(order);
		}

		return "redirect:/ui/report/accountorders";
	}

	@GetMapping("/ui/report/reports")
	public String reports(Model model) {
		ReportType[] reports = ReportType.values();

		// The S3 config
		String bucket = configuration.getS3().getBucketName();
		String path = configuration.getS3().getHistoricalReportsPath();

		boolean historicalReportsFeatureEnabled = configuration.getModules().getHistorialReportsOnOrgunits().isEnabled();
		if (historicalReportsFeatureEnabled) {
			List<String> ouReports = s3Service.list(bucket, path);
			model.addAttribute("ouReports", ouReports);
		}

		model.addAttribute("reports", reports);
		model.addAttribute("historicalReportsFeatureEnabled", historicalReportsFeatureEnabled);

		return "report/reports";
	}

	@GetMapping("/ui/report/reports/{reportType}")
	public String getReport(Model model, @PathVariable("reportType") ReportType report) {
		model.addAttribute("reportType", report);

		switch (report) {
			case AD_ACCOUNT_BUT_NO_AFFILIATION:
				model.addAttribute("persons", reportService.generateADWithoutAffiliationReport());
				return "report/ad_without_affiliation";
			case AD_ACCOUNT_BUT_NO_WAGES_AFFILIATION:
				model.addAttribute("persons", reportService.generateADWithoutActiveOpusAffiliationReport());
				return "report/ad_without_wages_affiliation";
			case DUPLICATE_AFFILIATION:
				model.addAttribute("persons", reportService.generateDuplicateAffiliationReport());
				return "report/duplicate_affiliation";
			case OPUS_ACCOUNT_BUT_NO_AD_ACCOUNT:
				model.addAttribute("persons", reportService.generateOpusButNoADReport());
				return "report/opus_without_ad";
			case PERSONS_DISABLE_ACCOUNT_ORDERS:
				model.addAttribute("persons", reportService.generateAccountOrdersDisabledReport());
				return "report/account_orders_disabled";
			case PERSONS_ON_LEAVE:
				model.addAttribute("persons", reportService.generatePersonsOnLeaveReport());
				return "report/persons_on_leave";
			case PERSONS_STOPPED:
				model.addAttribute("persons", reportService.generatePersonsForceStopReport());
				return "report/persons_force_stopped";
			case PERSONS_WITH_MULTIPLE_AFFILIATIONS:
				model.addAttribute("rows", reportService.generateMultipleAffiliationsReport());
				return "report/persons_multiple_affiliations";
			case PERSONS_WITH_SOFD_AFFILIATIONS:
				model.addAttribute("rows", reportService.generateSofdAffiliationsReport());
				return "report/persons_with_sofd_affiliations";
		}

		return "redirect:/ui/report/reports";
	}

	@GetMapping("/ui/report/historical_reports/{reportName:.+}")
	public ResponseEntity<?> downloadHistoricalReport(Model model, @PathVariable("reportName") String reportName) {

		boolean historicalReportsFeatureEnabled = configuration.getModules().getHistorialReportsOnOrgunits().isEnabled();
		if (!historicalReportsFeatureEnabled) {
			log.debug("This feature is disabled.");
			return ResponseEntity.badRequest().build();
		}

		// The S3 config
		String bucket = configuration.getS3().getBucketName();
		String path = configuration.getS3().getHistoricalReportsPath();

		try {
			byte[] bytes = s3Service.download(bucket, path + "/" + reportName);
	
			String fileName = URLEncoder.encode(reportName, "UTF-8").replaceAll("\\+", "%20");
	
			HttpHeaders httpHeaders = new HttpHeaders();
			httpHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);
			httpHeaders.setContentLength(bytes.length);
			httpHeaders.setContentDispositionFormData("attachment", fileName);
			
			return new ResponseEntity<>(bytes, httpHeaders, HttpStatus.OK);
		} catch (Exception e) {
			log.warn("Error occured while trying to download OrgUnit historical report. Filename: " + reportName, e);
		}

		return ResponseEntity.notFound().build();
	}

	@GetMapping("/ui/report/reports/{reportType}/download")
	public ModelAndView downloadReport(@PathVariable("reportType") ReportType report, HttpServletResponse response, Locale loc) {
		Map<String, Object> model = new HashMap<>();
		model.put("report", report);
		model.put("locale", loc);
		model.put("messagesBundle", messageSource);
		model.put("personService", personService);

		switch (report) {
			case AD_ACCOUNT_BUT_NO_AFFILIATION:
				model.put("persons", reportService.generateADWithoutAffiliationReport());
				break;
			case AD_ACCOUNT_BUT_NO_WAGES_AFFILIATION:
				model.put("persons", reportService.generateADWithoutActiveOpusAffiliationReport());
				break;
			case DUPLICATE_AFFILIATION:
				model.put("persons", reportService.generateDuplicateAffiliationReport());
				break;
			case OPUS_ACCOUNT_BUT_NO_AD_ACCOUNT:
				model.put("persons", reportService.generateOpusButNoADReport());
				break;
			case PERSONS_ON_LEAVE:
				model.put("persons", reportService.generatePersonsOnLeaveReport());
				break;
			case PERSONS_DISABLE_ACCOUNT_ORDERS:
				model.put("persons", reportService.generateAccountOrdersDisabledReport());
				break;
			case PERSONS_STOPPED:
				model.put("persons", reportService.generatePersonsForceStopReport());
				break;
			case PERSONS_WITH_MULTIPLE_AFFILIATIONS:
				model.put("rows", reportService.generateMultipleAffiliationsReport());
				response.setContentType("application/ms-excel");
				response.setHeader("Content-Disposition", "attachment; filename=\"rapport.xls\"");

				return new ModelAndView(new MultipleAffiliationsReportXlsView(), model);
			case PERSONS_WITH_SOFD_AFFILIATIONS:
				model.put("rows", reportService.generateSofdAffiliationsReport());
				response.setContentType("application/ms-excel");
				response.setHeader("Content-Disposition", "attachment; filename=\"rapport.xls\"");

				return new ModelAndView(new SofdAffiliationsReportXlsView(), model);
		}

		response.setContentType("application/ms-excel");
		response.setHeader("Content-Disposition", "attachment; filename=\"rapport.xls\"");

		return new ModelAndView(new GenericReportXlsView(), model);
	}
	
	@GetMapping(path = "/ui/report/notifications")
	public String listNotifications(Model model, HttpServletRequest request) {
		List<Notification> adminTasks = adminTasksService.findAll();
		
		Map<String, String> map = new HashMap<>();
		for (Notification adminTask : adminTasks) {
			map.put(adminTask.getNotificationType().toString(), messageSource.getMessage(adminTask.getNotificationType().getMessage(), null, new Locale("da-DK")));
		}

		Person person = personService.getLoggedInPerson();
		model.addAttribute("personUuid", (person != null) ? person.getUuid() : "");
		
		// update badge in UI
		long count = adminTasks.stream().filter(t -> t.isActive()).count();
		request.getSession().setAttribute(SessionConstants.SESSION_NOTIFICATION_COUNT, count);
		
		model.addAttribute("adminTasks", adminTasks);
		model.addAttribute("typesMap", map);

		return "report/notifications";
	}
	
	@GetMapping(path = "/ui/report/notifications/{id}")
	public String viewNotification(Model model, @PathVariable("id") long id) {
		Notification adminTask = adminTasksService.findById(id);
		if (adminTask == null) {
			return "redirect:/ui/report/notifications";
		}

		model.addAttribute("notification", adminTask);

		return "report/notificationView";
	}


}
