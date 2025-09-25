package dk.digitalidentity.sofd.controller.api;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dk.digitalidentity.sofd.controller.mvc.xls.ActiveAffiliationOrActiveAdAccountReportXlsView;
import dk.digitalidentity.sofd.controller.mvc.xls.UsersReportXlsView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.controller.mvc.xls.AccountOrderRulesXlsDto;
import dk.digitalidentity.sofd.controller.mvc.xls.AccountOrderRulesXlsView;
import dk.digitalidentity.sofd.controller.mvc.xls.PersonsWithActiveSOFDAffiliationsReportXlsView;
import dk.digitalidentity.sofd.controller.mvc.xls.PersonsWithAffiliationsWorkplacesReportXlsView;
import dk.digitalidentity.sofd.controller.mvc.xls.GenericReportXlsView;
import dk.digitalidentity.sofd.controller.mvc.xls.MultipleAffiliationsReportXlsView;
import dk.digitalidentity.sofd.controller.mvc.xls.SofdAffiliationsReportXlsView;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.enums.ReportType;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.service.AccountOrderService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.ReportService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import org.springframework.web.servlet.ModelAndView;

@RestController
@RequireReadAccess
public class DownloadExcelApi {

	@Autowired
	private OrgUnitService orgUnitService;

	@Autowired
	private AccountOrderService accountOrderService;
	
	@Autowired
	private SupportedUserTypeService supportedUserTypeService;
	
	@Autowired
	private MessageSource messageSource;
	
	@Autowired
	private PersonService personService;
	
	@Autowired
	private ReportService reportService;

	@GetMapping("/api/excel/accountOrderRules")
	public void downloadAccountOrderRules(Locale loc, HttpServletRequest request, HttpServletResponse response) throws Exception {
		AccountOrderRulesXlsDto dto = new AccountOrderRulesXlsDto();
		for (OrgUnit orgUnit : orgUnitService.getAllActive()) {
			dto.add(orgUnit, accountOrderService.getAccountOrderSettings(orgUnit, true));
		}

		Map<String, Object> model = new HashMap<>();
		model.put("employees", dto);
		model.put("supportedUserTypeService", supportedUserTypeService);
		model.put("messagesBundle", messageSource);
		model.put("locale", loc);

		response.setContentType("application/ms-excel");
		response.setHeader("Content-Disposition", "attachment; filename=\"regler.xlsx\"");

		new AccountOrderRulesXlsView().render(model, request, response);
	}

	@GetMapping("/api/excel/reports")
	public ResponseEntity<Map<String, String>> listReports(Locale loc) {
		ReportType[] reports = ReportType.values();
		Map<String, String> response = new HashMap<>();

		for (ReportType reportType : reports) {
			response.put(reportType.name(), messageSource.getMessage(reportType.getTitle(), null, loc));
		}

		return ResponseEntity.ok(response);
	}

	@GetMapping("/api/excel/report/{reportType}")
	public void downloadReport(@PathVariable("reportType") ReportType report, Locale loc, HttpServletRequest request, HttpServletResponse response) throws Exception {
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
				response.setHeader("Content-Disposition", "attachment; filename=\"rapport.xlsx\"");

				new MultipleAffiliationsReportXlsView().render(model, request, response);
				return;
			case PERSONS_WITH_SOFD_AFFILIATIONS:
				model.put("rows", reportService.generateSofdAffiliationsReport());
				response.setContentType("application/ms-excel");
				response.setHeader("Content-Disposition", "attachment; filename=\"rapport.xlsx\"");

				new SofdAffiliationsReportXlsView().render(model, request, response);
				return;
			case PERSONS_WITH_ACTIVE_SOFD_AFFILIATIONS:
				model.put("rows", reportService.generatePersonsWithActiveSOFDAffiliationsReport());
				response.setContentType("application/ms-excel");
				response.setHeader("Content-Disposition", "attachment; filename=\"rapport.xlsx\"");

				new PersonsWithActiveSOFDAffiliationsReportXlsView().render(model, request, response);
				return;
			case ACTIVE_AFFILIATION_OR_ACTIVE_AD_ACCOUNT:
				model.put("rows", reportService.generateActiveAffiliationOrActiveADAccountReport());
				response.setContentType("application/ms-excel");
				response.setHeader("Content-Disposition", "attachment; filename=\"rapport.xlsx\"");

				new ActiveAffiliationOrActiveAdAccountReportXlsView().render(model, request, response);
				return;
			case PERSONS_WITH_AFFILIATIONS_WORKPLACES:
				model.put("rows", reportService.generatePersonWithAffiliationsWorkplacesReport());
				response.setContentType("application/ms-excel");
				response.setHeader("Content-Disposition", "attachment; filename=\"rapport.xlsx\"");
				
				new PersonsWithAffiliationsWorkplacesReportXlsView().render(model, request, response);
				return;
		}

		response.setContentType("application/ms-excel");
		response.setHeader("Content-Disposition", "attachment; filename=\"rapport.xlsx\"");

		new GenericReportXlsView().render(model, request, response);
	}

	@GetMapping("/api/excel/adusers")
	public ModelAndView downloadUsersReport(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, HttpServletResponse response, Locale loc) throws Exception {
		Map<String, Object> model = new HashMap<>();
		model.put("locale", loc);
		model.put("messagesBundle", messageSource);
		model.put("personService", personService);
		model.put("rows", reportService.generateADUsersReport(date));

		response.setContentType("application/ms-excel");
		response.setHeader("Content-Disposition", "attachment; filename=\"rapport.xlsx\"");

		return new ModelAndView(new UsersReportXlsView(), model);
	}


}
