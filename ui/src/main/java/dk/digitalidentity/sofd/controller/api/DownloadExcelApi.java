package dk.digitalidentity.sofd.controller.api;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.xls.AccountOrderRulesXlsDto;
import dk.digitalidentity.sofd.controller.mvc.xls.AccountOrderRulesXlsView;
import dk.digitalidentity.sofd.controller.mvc.xls.ManagersReportXlsView;
import dk.digitalidentity.sofd.controller.mvc.xls.EmployeesInformationXlsView;
import dk.digitalidentity.sofd.controller.mvc.xls.ActiveAffiliationOrActiveAdAccountReportXlsView;
import dk.digitalidentity.sofd.controller.mvc.xls.GenericReportXlsView;
import dk.digitalidentity.sofd.controller.mvc.xls.MultipleAffiliationsReportXlsView;
import dk.digitalidentity.sofd.controller.mvc.xls.PersonsWithActiveSOFDAffiliationsReportXlsView;
import dk.digitalidentity.sofd.controller.mvc.xls.PersonsWithAffiliationsWorkplacesReportXlsView;
import dk.digitalidentity.sofd.controller.mvc.xls.SofdAffiliationsReportXlsView;
import dk.digitalidentity.sofd.controller.mvc.xls.UsersReportXlsView;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.enums.ReportType;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.service.AccountOrderService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.ReportService;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequireReadAccess
@Tag(name = "Excel", description = "API til hentning af Excel-rapporter")
public class DownloadExcelApi {

	private static final String ORG_UNIT_EMPLOYEES = "ORG_UNIT_EMPLOYEES";
	private static final String ORG_UNIT_EMPLOYEES_NESTED = "ORG_UNIT_EMPLOYEES_NESTED";
	private static final String MANAGERS = "MANAGERS";

	@Autowired
	private SofdConfiguration configuration;

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

	@Operation(summary = "Hent kontoordre-regler", description = "Returnerer en Excel-fil med kontoordre-regler for alle aktive enheder")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Excel-fil med kontoordre-regler")
	})
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

		new AccountOrderRulesXlsView("regler.xlsx").render(model, request, response);
	}

	@Operation(summary = "List tilgængelige rapporttyper", description = "Returnerer en liste af tilgængelige rapporttyper med deres navne")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Liste af rapporttyper")
	})
	@GetMapping("/api/excel/reports")
	public ResponseEntity<Map<String, String>> listReports(Locale loc) {
		Map<String, String> response = new HashMap<>();

		for (ReportType reportType : ReportType.values()) {
			response.put(reportType.name(), messageSource.getMessage(reportType.getTitle(), null, loc));
		}

		response.put(ORG_UNIT_EMPLOYEES, messageSource.getMessage("html.report.orgunit_employees", null, loc));
		response.put(ORG_UNIT_EMPLOYEES_NESTED, messageSource.getMessage("html.report.orgunit_employees_nested", null, loc));
		response.put(MANAGERS, messageSource.getMessage("html.managers.list.headline", null, loc));

		return ResponseEntity.ok(response);
	}

	@Operation(summary = "Hent rapport", description = "Returnerer en Excel-rapport af den angivne type. For ORG_UNIT_EMPLOYEES og ORG_UNIT_EMPLOYEES_NESTED skal orgUnitUuid angives")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Excel-rapport"),
		@ApiResponse(responseCode = "400", description = "Ukendt rapporttype eller manglende orgUnitUuid"),
		@ApiResponse(responseCode = "404", description = "Enhed ikke fundet")
	})
	@GetMapping("/api/excel/report/{reportType}")
	public void downloadReport(
			@Parameter(description = "Rapporttype - se /api/excel/reports for mulige værdier") @PathVariable("reportType") String reportType,
			@Parameter(description = "UUID på enheden - påkrævet for ORG_UNIT_EMPLOYEES og ORG_UNIT_EMPLOYEES_NESTED") @RequestParam(required = false) String orgUnitUuid,
			Locale loc, HttpServletRequest request, HttpServletResponse response) throws Exception {

		// managers report
		if (MANAGERS.equals(reportType)) {
			boolean orgUnitSubstituteEnabled = configuration.getModules().getOrgUnitSubstitute().isEnabled();
			Map<String, Object> model = new HashMap<>();
			model.put("rows", reportService.generateManagersReport(orgUnitSubstituteEnabled));
			model.put("orgUnitSubstituteEnabled", orgUnitSubstituteEnabled);
			model.put("messagesBundle", messageSource);
			model.put("locale", loc);
			new ManagersReportXlsView("Ledere.xlsx").render(model, request, response);
			return;
		}

		// org unit reports
		if (ORG_UNIT_EMPLOYEES.equals(reportType) || ORG_UNIT_EMPLOYEES_NESTED.equals(reportType)) {
			if (orgUnitUuid == null) {
				throw new IllegalArgumentException("orgUnitUuid parameter required for this report type");
			}
			OrgUnit orgUnit = orgUnitService.getByUuid(orgUnitUuid);
			if (orgUnit == null) {
				throw new IllegalArgumentException("OrgUnit not found: " + orgUnitUuid);
			}

			Map<String, Object> model = new HashMap<>();
			model.put("employees", ORG_UNIT_EMPLOYEES.equals(reportType)
					? orgUnitService.getEmployeesWithUsers(orgUnit)
					: orgUnitService.getEmployeesWithUsersRecursive(orgUnit));
			model.put("supportedUserTypeService", supportedUserTypeService);
			model.put("messagesBundle", messageSource);
			model.put("locale", loc);

			new EmployeesInformationXlsView("Medarbejderoplysninger - " + orgUnit.getName() + ".xlsx").render(model, request, response);
			return;
		}

		// standard reports
		ReportType report;
		try {
			report = ReportType.valueOf(reportType);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("Unknown report type: " + reportType);
		}

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
				new MultipleAffiliationsReportXlsView("rapport.xlsx").render(model, request, response);
				return;
			case PERSONS_WITH_SOFD_AFFILIATIONS:
				model.put("rows", reportService.generateSofdAffiliationsReport());
				new SofdAffiliationsReportXlsView("rapport.xlsx").render(model, request, response);
				return;
			case PERSONS_WITH_ACTIVE_SOFD_AFFILIATIONS:
				model.put("rows", reportService.generatePersonsWithActiveSOFDAffiliationsReport());
				new PersonsWithActiveSOFDAffiliationsReportXlsView("rapport.xlsx").render(model, request, response);
				return;
			case ACTIVE_AFFILIATION_OR_ACTIVE_AD_ACCOUNT:
				model.put("rows", reportService.generateActiveAffiliationOrActiveADAccountReport());
				new ActiveAffiliationOrActiveAdAccountReportXlsView("rapport.xlsx").render(model, request, response);
				return;
			case PERSONS_WITH_AFFILIATIONS_WORKPLACES:
				model.put("rows", reportService.generatePersonWithAffiliationsWorkplacesReport());
				new PersonsWithAffiliationsWorkplacesReportXlsView("rapport.xlsx").render(model, request, response);
				return;
		}

		new GenericReportXlsView("rapport.xlsx").render(model, request, response);
	}

	@Operation(summary = "Hent AD-brugerrapport", description = "Returnerer en Excel-rapport over AD-brugere fra en given dato og frem til i dag")
	@ApiResponses(value = {
		@ApiResponse(responseCode = "200", description = "Excel-rapport over AD-brugere")
	})
	@GetMapping("/api/excel/adusers")
	public ModelAndView downloadUsersReport(
			@Parameter(description = "Startdato (ISO format: yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			HttpServletResponse response, Locale loc) throws Exception {
		Map<String, Object> model = new HashMap<>();
		model.put("locale", loc);
		model.put("messagesBundle", messageSource);
		model.put("personService", personService);
		model.put("rows", reportService.generateADUsersReport(date));

		return new ModelAndView(new UsersReportXlsView("rapport.xlsx"), model);
	}
}
