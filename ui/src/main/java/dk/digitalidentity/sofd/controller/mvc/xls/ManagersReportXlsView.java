package dk.digitalidentity.sofd.controller.mvc.xls;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.View;

import dk.digitalidentity.sofd.controller.mvc.dto.ManagerReportDTO;
import dk.digitalidentity.sofd.dao.model.enums.OrgUnitManagerSource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ManagersReportXlsView implements View {
	private static final String CONTENT_TYPE = "application/ms-excel";
	private final String filename;

	public ManagersReportXlsView(String filename) {
		this.filename = filename;
	}

	@Override
	public String getContentType() {
		return CONTENT_TYPE;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		List<ManagerReportDTO> rows = (List<ManagerReportDTO>) model.get("rows");
		boolean orgUnitSubstituteEnabled = Boolean.TRUE.equals(model.get("orgUnitSubstituteEnabled"));
		Locale locale = (Locale) model.get("locale");
		ResourceBundleMessageSource messageSource = (ResourceBundleMessageSource) model.get("messagesBundle");

		response.setContentType(getContentType());
		response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

		try (Workbook workbook = new DisposableSXSSFWorkbook()) {
			Sheet sheet = workbook.createSheet(messageSource.getMessage("xls.genericreport.sheetname", null, locale));
			((SXSSFSheet) sheet).trackAllColumnsForAutoSizing();

			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			CellStyle headerStyle = workbook.createCellStyle();
			headerStyle.setFont(headerFont);

			Row header = sheet.createRow(0);
			createCell(header, 0, messageSource.getMessage("html.manager.name", null, locale), headerStyle);
			createCell(header, 1, messageSource.getMessage("html.manager.ous", null, locale), headerStyle);
			createCell(header, 2, messageSource.getMessage("html.manager.source", null, locale), headerStyle);
			createCell(header, 3, messageSource.getMessage("html.manager.inherited", null, locale), headerStyle);
			createCell(header, 4, messageSource.getMessage("html.manager.substitutes", null, locale), headerStyle);
			if (orgUnitSubstituteEnabled) {
				createCell(header, 5, messageSource.getMessage("html.manager.orgunit.substitutes", null, locale), headerStyle);
			}

			int rowCount = 1;
			for (ManagerReportDTO row : rows) {
				Row dataRow = sheet.createRow(rowCount++);
				dataRow.createCell(0).setCellValue(row.getName());
				dataRow.createCell(1).setCellValue(row.getOrgUnitName());
				dataRow.createCell(2).setCellValue(row.getSource() == OrgUnitManagerSource.SELECTED
						? messageSource.getMessage("html.manager.source.selected", null, locale)
						: messageSource.getMessage("html.manager.source.imported", null, locale));
				dataRow.createCell(3).setCellValue(row.isInherited() ? "Ja" : "Nej");
				dataRow.createCell(4).setCellValue(String.join(", ", row.getSubstitutes()));
				if (orgUnitSubstituteEnabled) {
					dataRow.createCell(5).setCellValue(String.join(", ", row.getOrgUnitSubstitutes()));
				}
			}

			for (int i = 0; i <= (orgUnitSubstituteEnabled ? 5 : 4); i++) {
				sheet.autoSizeColumn(i);
			}

			workbook.write(response.getOutputStream());
		}
	}

	private static void createCell(Row row, int column, String value, CellStyle style) {
		Cell cell = row.createCell(column);
		cell.setCellValue(value);
		cell.setCellStyle(style);
	}
}
