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

import dk.digitalidentity.sofd.controller.mvc.dto.SofdAffiliationsReportDTO;
import dk.digitalidentity.sofd.dao.model.enums.ReportType;
import org.springframework.web.servlet.view.document.AbstractXlsxView;

public class SofdAffiliationsReportXlsView extends AbstractXlsxView {

	@SuppressWarnings("unchecked")
	@Override
	protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception {
		ReportType reportType = (ReportType) model.get("report");
		List<SofdAffiliationsReportDTO> rows = (List<SofdAffiliationsReportDTO>) model.get("rows");
		Locale locale = (Locale) model.get("locale");
		ResourceBundleMessageSource messageSource = (ResourceBundleMessageSource) model.get("messagesBundle");

		// create excel xls sheet
		Sheet sheet = workbook.createSheet(messageSource.getMessage("xls.genericreport.sheetname", null, locale));

		// create header row
		createHeader(workbook, sheet, reportType, messageSource, locale);

		// Create data cells
		int rowCount = 1;
		for (SofdAffiliationsReportDTO row : rows) {
			Row courseRow = sheet.createRow(rowCount++);

			courseRow.createCell(0).setCellValue(row.getName());
			courseRow.createCell(1).setCellValue(row.getCpr());
			courseRow.createCell(2).setCellValue(row.getAffiliationName());
			courseRow.createCell(3).setCellValue(row.getAffiliationOrgUnitName());
			courseRow.createCell(4).setCellValue(row.getAffiliationVendor());
			courseRow.createCell(5).setCellValue(row.getAffiliationStartDate());
			courseRow.createCell(6).setCellValue(row.getAffiliationStopDate());
		}

		format(sheet);
	}

	private void format(Sheet sheet) {
		sheet.autoSizeColumn(0);
		sheet.autoSizeColumn(1);
		sheet.autoSizeColumn(2);
		sheet.autoSizeColumn(3);
		sheet.autoSizeColumn(4);
		sheet.autoSizeColumn(5);
		sheet.autoSizeColumn(6);
	}

	private void createHeader(Workbook workbook, Sheet sheet, ReportType reportType, ResourceBundleMessageSource messageSource, Locale locale) {
		String header1 = messageSource.getMessage("xls.genericreport.person.name", null, locale);
		String header2 = messageSource.getMessage("xls.genericreport.person.cpr", null, locale);
		String header3 = messageSource.getMessage("xls.genericreport.person.affiliation", null, locale);
		String header4 = messageSource.getMessage("xls.genericreport.affiliation.unit", null, locale);
		String header5 = messageSource.getMessage("xls.genericreport.affiliation.vendor", null, locale);
		String header6 = messageSource.getMessage("xls.genericreport.affiliation.startDate", null, locale);
		String header7 = messageSource.getMessage("xls.genericreport.affiliation.stopDate", null, locale);

		Font headerFont = workbook.createFont();
		headerFont.setBold(true);

		CellStyle headerStyle = workbook.createCellStyle();
		headerStyle.setFont(headerFont);

		Row header = sheet.createRow(0);
		createCell(header, 0, header1, headerStyle);
		createCell(header, 1, header2, headerStyle);
		createCell(header, 2, header3, headerStyle);
		createCell(header, 3, header4, headerStyle);
		createCell(header, 4, header5, headerStyle);
		createCell(header, 5, header6, headerStyle);
		createCell(header, 6, header7, headerStyle);
	}

	private static void createCell(Row header, int column, String value, CellStyle style) {
		Cell cell = header.createCell(column);
		cell.setCellValue(value);
		cell.setCellStyle(style);
	}
}
