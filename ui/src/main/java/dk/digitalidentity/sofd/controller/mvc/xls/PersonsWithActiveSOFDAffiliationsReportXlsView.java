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

import dk.digitalidentity.sofd.controller.mvc.dto.PersonWithActiveSOFDAffiliationsReportDTO;
import dk.digitalidentity.sofd.dao.model.enums.ReportType;
import org.springframework.web.servlet.view.document.AbstractXlsxView;

public class PersonsWithActiveSOFDAffiliationsReportXlsView extends AbstractXlsxView {

	@SuppressWarnings("unchecked")
	@Override
	protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception {
		ReportType reportType = (ReportType) model.get("report");
		List<PersonWithActiveSOFDAffiliationsReportDTO> rows = (List<PersonWithActiveSOFDAffiliationsReportDTO>) model.get("rows");
		Locale locale = (Locale) model.get("locale");
		ResourceBundleMessageSource messageSource = (ResourceBundleMessageSource) model.get("messagesBundle");

		// create excel xls sheet
		Sheet sheet = workbook.createSheet(messageSource.getMessage("xls.genericreport.sheetname", null, locale));

		// create header row
		createHeader(workbook, sheet, reportType, messageSource, locale);

		// Create data cells
		int rowCount = 1;
		for (PersonWithActiveSOFDAffiliationsReportDTO row : rows) {
			Row courseRow = sheet.createRow(rowCount++);

			courseRow.createCell(0).setCellValue(row.getName());
			courseRow.createCell(1).setCellValue(row.getUserId());
			courseRow.createCell(2).setCellValue(row.getAffiliationName());
			courseRow.createCell(3).setCellValue(row.getAffiliationOrgUnitName());
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
		String header2 = messageSource.getMessage("xls.genericreport.person.ad", null, locale);
		String header3 = messageSource.getMessage("xls.genericreport.person.affiliation", null, locale);
		String header4 = messageSource.getMessage("xls.genericreport.affiliation.unit", null, locale);

		Font headerFont = workbook.createFont();
		headerFont.setBold(true);

		CellStyle headerStyle = workbook.createCellStyle();
		headerStyle.setFont(headerFont);

		Row header = sheet.createRow(0);
		createCell(header, 0, header1, headerStyle);
		createCell(header, 1, header2, headerStyle);
		createCell(header, 2, header3, headerStyle);
		createCell(header, 3, header4, headerStyle);
	}

	private static void createCell(Row header, int column, String value, CellStyle style) {
		Cell cell = header.createCell(column);
		cell.setCellValue(value);
		cell.setCellStyle(style);
	}
}
