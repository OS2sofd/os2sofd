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

import dk.digitalidentity.sofd.dao.model.enums.ReportType;
import dk.digitalidentity.sofd.controller.mvc.dto.PersonWithAffiliationsWorkplacesReportDTO;

public class PersonsWithAffiliationsWorkplacesReportXlsView extends AbstractXlsxView {
	
	@SuppressWarnings("unchecked")
	@Override
	protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception {
		ReportType reportType = (ReportType) model.get("report");
		var rows = (List<PersonWithAffiliationsWorkplacesReportDTO>) model.get("rows");
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
			excelRow.createCell(1).setCellValue(row.getEmployeeNumber());
			excelRow.createCell(2).setCellValue(row.getPositionName());
			excelRow.createCell(3).setCellValue(row.getAffiliationStartDate());
			excelRow.createCell(4).setCellValue(row.getAffiliationStopDate());
			excelRow.createCell(5).setCellValue(row.getAffiliationPlacement());
			excelRow.createCell(6).setCellValue(row.getWorkplace());
			excelRow.createCell(7).setCellValue(row.getWorkplaceStartDate());
			excelRow.createCell(8).setCellValue(row.getWorkplaceStopDate());
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
		sheet.autoSizeColumn(7);
		sheet.autoSizeColumn(8);
	}
	
	private void createHeader(Workbook workbook, Sheet sheet, ReportType reportType, ResourceBundleMessageSource messageSource, Locale locale) {
		String header1 = messageSource.getMessage("xls.genericreport.person.name", null, locale);
		String header2 = messageSource.getMessage("xls.employees.header.employee_number", null, locale);
		String header3 = messageSource.getMessage("xls.employees.header.position.name", null, locale);
		String header4 = messageSource.getMessage("xls.employees.header.start", null, locale);
		String header5 = messageSource.getMessage("xls.employees.header.stop", null, locale);
		String header6 = messageSource.getMessage("xls.employees.header.orgunit", null, locale);
		String header7 = messageSource.getMessage("xls.workplaces.header.workplace", null, locale);
		String header8 = messageSource.getMessage("xls.workplaces.header.workplace.start", null, locale);
		String header9 = messageSource.getMessage("xls.workplaces.header.workplace.stop", null, locale);

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
		createCell(header, 7, header8, headerStyle);
		createCell(header, 8, header9, headerStyle);
	}

	private static void createCell(Row row, int column, String value, CellStyle style) {
		Cell cell = row.createCell(column);
		cell.setCellValue(value);
		cell.setCellStyle(style);
	}
	

}
