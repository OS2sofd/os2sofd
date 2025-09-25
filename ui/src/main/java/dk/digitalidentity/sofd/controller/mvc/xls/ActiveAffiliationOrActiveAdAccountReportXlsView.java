package dk.digitalidentity.sofd.controller.mvc.xls;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.view.document.AbstractXlsxView;

import dk.digitalidentity.sofd.controller.mvc.dto.ActiveAffiliationOrActiveADAccountReportDTO;
import dk.digitalidentity.sofd.dao.model.enums.ReportType;

public class ActiveAffiliationOrActiveAdAccountReportXlsView extends AbstractXlsxView {

	@SuppressWarnings("unchecked")
	@Override
	protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception {
		ReportType reportType = (ReportType) model.get("report");
		var rows = (List<ActiveAffiliationOrActiveADAccountReportDTO>) model.get("rows");
		Locale locale = (Locale) model.get("locale");
		ResourceBundleMessageSource messageSource = (ResourceBundleMessageSource) model.get("messagesBundle");

		// create excel xls sheet
		Sheet sheet = workbook.createSheet(messageSource.getMessage("xls.genericreport.sheetname", null, locale));

		// create header row
		createHeader(workbook, sheet, reportType, messageSource, locale);

		// Create data cells
		int rowCount = 1;
		for (var row : rows) {
			CellStyle wrapStyle = workbook.createCellStyle();
			wrapStyle.setWrapText(true);

			Row excelRow = sheet.createRow(rowCount++);

			excelRow.createCell(0).setCellValue(row.getName());
			createCell(excelRow, 1, String.join("\n", row.getUsers()), wrapStyle);
			createCell(excelRow, 2, String.join("\n", row.getAffiliations()), wrapStyle);
		}

		format(sheet);
	}

	private void format(Sheet sheet) {
		sheet.autoSizeColumn(0);
		sheet.autoSizeColumn(1);
		sheet.autoSizeColumn(2);
	}

	private void createHeader(Workbook workbook, Sheet sheet, ReportType reportType, ResourceBundleMessageSource messageSource, Locale locale) {
		String header1 = messageSource.getMessage("xls.genericreport.person.name", null, locale);
		String header2 = messageSource.getMessage("html.report.active_affiliation_or_active_ad.active_users", null, locale);
		String header3 = messageSource.getMessage("html.report.active_affiliation_or_active_ad.active_affiliations", null, locale);

		Font headerFont = workbook.createFont();
		headerFont.setBold(true);

		CellStyle headerStyle = workbook.createCellStyle();
		headerStyle.setFont(headerFont);

		Row header = sheet.createRow(0);
		createCell(header, 0, header1, headerStyle);
		createCell(header, 1, header2, headerStyle);
		createCell(header, 2, header3, headerStyle);
	}

	private static void createCell(Row row, int column, String value, CellStyle style) {
		Cell cell = row.createCell(column);
		cell.setCellValue(value);
		cell.setCellStyle(style);
	}
}
