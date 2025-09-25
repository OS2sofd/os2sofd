package dk.digitalidentity.sofd.controller.mvc.xls;

import java.text.SimpleDateFormat;
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

import dk.digitalidentity.sofd.dao.model.AuditLog;
import org.springframework.web.servlet.view.document.AbstractXlsxView;

public class AuditLogXlsView extends AbstractXlsxView {

	@SuppressWarnings("unchecked")
	@Override
	protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception {
		Locale locale = (Locale) model.get("locale");
		Iterable<AuditLog> logs = (Iterable<AuditLog>) model.get("logs");
		ResourceBundleMessageSource messageSource = (ResourceBundleMessageSource) model.get("messagesBundle");
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		// create excel xls sheet
		Sheet sheet = workbook.createSheet(messageSource.getMessage("xls.rules.sheetname", null, locale));

		// create header row
		createHeader(workbook, sheet, messageSource, locale);

		// Create data cells
		int row = 1;
		for (AuditLog log : logs) {
			Row dataRow = sheet.createRow(row++);

			String eventType = messageSource.getMessage(log.getEventType().getMessage(), null, locale);
			String entityType = messageSource.getMessage(log.getEntityType().getMessage(), null, locale);

			createCell(dataRow, 0, dateFormatter.format(log.getTimestamp()), null);
			createCell(dataRow, 1, log.getUserId(), null);
			createCell(dataRow, 2, eventType, null);
			createCell(dataRow, 3, entityType, null);
			createCell(dataRow, 4, log.getEntityId(), null);
			createCell(dataRow, 5, log.getEntityName(), null);
			createCell(dataRow, 6, log.getMessage(), null);
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

	private void createHeader(Workbook workbook, Sheet sheet, ResourceBundleMessageSource messageSource, Locale locale) {
		String hTimestamp = messageSource.getMessage("xls.auditlog.timestamp", null, locale);
		String hUserId = messageSource.getMessage("xls.auditlog.userId", null, locale);
		String hEventType = messageSource.getMessage("xls.auditlog.eventType", null, locale);
		String hEntityType = messageSource.getMessage("xls.auditlog.entityType", null, locale);
		String hEntityId = messageSource.getMessage("xls.auditlog.entityId", null, locale);
		String hEntityName = messageSource.getMessage("xls.auditlog.entityName", null, locale);
		String hMessage = messageSource.getMessage("xls.auditlog.message", null, locale);

		Font headerFont = workbook.createFont();
		headerFont.setBold(true);

		CellStyle headerStyle = workbook.createCellStyle();
		headerStyle.setFont(headerFont);

		Row header = sheet.createRow(0);
		createCell(header, 0, hTimestamp, headerStyle);
		createCell(header, 1, hUserId, headerStyle);
		createCell(header, 2, hEventType, headerStyle);
		createCell(header, 3, hEntityType, headerStyle);
		createCell(header, 4, hEntityId, headerStyle);
		createCell(header, 5, hEntityName, headerStyle);
		createCell(header, 6, hMessage, headerStyle);
	}

	private static void createCell(Row header, int column, String value, CellStyle style) {
		Cell cell = header.createCell(column);
		cell.setCellValue(value);
		cell.setCellStyle(style);
	}
}
