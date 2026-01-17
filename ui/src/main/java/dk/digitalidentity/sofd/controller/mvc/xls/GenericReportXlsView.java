package dk.digitalidentity.sofd.controller.mvc.xls;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import dk.digitalidentity.sofd.service.AffiliationService;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.context.support.ResourceBundleMessageSource;

import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.enums.ReportType;
import dk.digitalidentity.sofd.service.PersonService;
import org.springframework.web.servlet.view.document.AbstractXlsxView;

public class GenericReportXlsView extends AbstractXlsxView {

	@SuppressWarnings("unchecked")
	@Override
	protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception {
		ReportType reportType = (ReportType) model.get("report");
		List<Person> persons = (List<Person>) model.get("persons");
		Locale locale = (Locale) model.get("locale");
		ResourceBundleMessageSource messageSource = (ResourceBundleMessageSource) model.get("messagesBundle");
		PersonService personService = (PersonService) model.get("personService");
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");

		// create excel xls sheet
		Sheet sheet = workbook.createSheet(messageSource.getMessage("xls.genericreport.sheetname", null, locale));

		// create header row
		createHeader(workbook, sheet, reportType, messageSource, locale);

		// Create data cells
		int rowCount = 1;
		for (Person person : persons) {
			Row courseRow = sheet.createRow(rowCount++);

			courseRow.createCell(0).setCellValue(person.getFirstname() + " " + person.getSurname());
			courseRow.createCell(1).setCellValue(PersonService.maskCpr(person.getCpr()));

			Affiliation affiliation = person.getPrimeAffiliation();
			if (affiliation != null) {
				courseRow.createCell(2).setCellValue(AffiliationService.getPositionName(affiliation) + " i " + affiliation.getCalculatedOrgUnit().getName());
			}
			else {
				courseRow.createCell(2).setCellValue("");
			}

			switch (reportType) {
				case PERSONS_WITH_MULTIPLE_AFFILIATIONS:
				case PERSONS_WITH_SOFD_AFFILIATIONS:
				case PERSONS_WITH_ACTIVE_SOFD_AFFILIATIONS:
				case ACTIVE_AFFILIATION_OR_ACTIVE_AD_ACCOUNT:
					throw new RuntimeException("Wrong Xls View used");
				case PERSONS_STOPPED:
					courseRow.createCell(3).setCellValue(person.getStopReason());
					break;
				case PERSONS_DISABLE_ACCOUNT_ORDERS:
				case DUPLICATE_AFFILIATION:
					break;
				case AD_ACCOUNT_BUT_NO_AFFILIATION:
				case AD_ACCOUNT_BUT_NO_WAGES_AFFILIATION:
					courseRow.createCell(3).setCellValue(person.getActiveADAccounts());
					courseRow.createCell(4).setCellValue(personService.hasAffiliationInWagesSystem(person) ? messageSource.getMessage("html.button.yes", null, locale) : messageSource.getMessage("html.button.no", null, locale));
					break;
				case OPUS_ACCOUNT_BUT_NO_AD_ACCOUNT:
					courseRow.createCell(3).setCellValue(person.getPrimeOPUSAccount());
					break;
				case PERSONS_ON_LEAVE:
					if (person.getLeave() != null) {
						courseRow.createCell(3).setCellValue(messageSource.getMessage(person.getLeave().getReason().getMessage(), null, locale));
						courseRow.createCell(4).setCellValue(person.getLeave().getReasonText());
						courseRow.createCell(5).setCellValue((person.getLeave().getStartDate() != null) ? formatter.format(person.getLeave().getStartDate()) : "");
						courseRow.createCell(6).setCellValue((person.getLeave().getStopDate() != null) ? formatter.format(person.getLeave().getStopDate()) : "");
					}
					break;
				default:
					throw new Exception("The case " + reportType.name() + " is missing in GenericReportXlsView.buildExcelDocument");
			}
		}

		format(sheet);
	}

	private void format(Sheet sheet) {
		sheet.autoSizeColumn(0);
		sheet.autoSizeColumn(1);
		sheet.autoSizeColumn(2);
		sheet.autoSizeColumn(3);
	}

	private void createHeader(Workbook workbook, Sheet sheet, ReportType reportType, ResourceBundleMessageSource messageSource, Locale locale) {
		String header1 = messageSource.getMessage("xls.genericreport.person.name", null, locale);
		String header2 = messageSource.getMessage("xls.genericreport.person.cpr", null, locale);
		String header3 = messageSource.getMessage("xls.genericreport.person.affiliation", null, locale);
		String header4 = null;
		String header5 = null;
		String header6 = null;
		String header7 = null;

		switch (reportType) {
			case PERSONS_WITH_MULTIPLE_AFFILIATIONS:
			case PERSONS_WITH_SOFD_AFFILIATIONS:
			case PERSONS_WITH_ACTIVE_SOFD_AFFILIATIONS:
			case ACTIVE_AFFILIATION_OR_ACTIVE_AD_ACCOUNT:
				throw new RuntimeException("Wrong Xls View used");
			case PERSONS_STOPPED:
				header4 = messageSource.getMessage("xls.genericreport.person.stopReason", null, locale);
				break;
			case PERSONS_DISABLE_ACCOUNT_ORDERS:
			case DUPLICATE_AFFILIATION:
				break;
			case AD_ACCOUNT_BUT_NO_AFFILIATION:
			case AD_ACCOUNT_BUT_NO_WAGES_AFFILIATION:
				header4 = messageSource.getMessage("xls.genericreport.person.ad", null, locale);
				header5 = messageSource.getMessage("xls.adaccountnoaffiliationreport.fromwages", null, locale);
				break;
			case OPUS_ACCOUNT_BUT_NO_AD_ACCOUNT:
				header4 = messageSource.getMessage("xls.genericreport.person.opus", null, locale);
				break;
			case PERSONS_ON_LEAVE:
				header4 = messageSource.getMessage("xls.genericreport.person.leaveReason", null, locale);
				header5 = messageSource.getMessage("xls.genericreport.person.leaveReasonText", null, locale);
				header6 = messageSource.getMessage("xls.genericreport.person.leaveStartDate", null, locale);
				header7 = messageSource.getMessage("xls.genericreport.person.leaveStopDate", null, locale);
				break;
		default:
			break;
		}

		Font headerFont = workbook.createFont();
		headerFont.setBold(true);

		CellStyle headerStyle = workbook.createCellStyle();
		headerStyle.setFont(headerFont);

		Row header = sheet.createRow(0);
		createCell(header, 0, header1, headerStyle);
		createCell(header, 1, header2, headerStyle);
		createCell(header, 2, header3, headerStyle);
		if (header4 != null) {
			createCell(header, 3, header4, headerStyle);
		}
		if (header5 != null) {
			createCell(header, 4, header5, headerStyle);
		}
		if (header6 != null) {
			createCell(header, 5, header6, headerStyle);
		}
		if (header7 != null) {
			createCell(header, 6, header7, headerStyle);
		}
	}

	private static void createCell(Row header, int column, String value, CellStyle style) {
		Cell cell = header.createCell(column);
		cell.setCellValue(value);
		cell.setCellStyle(style);
	}
}
