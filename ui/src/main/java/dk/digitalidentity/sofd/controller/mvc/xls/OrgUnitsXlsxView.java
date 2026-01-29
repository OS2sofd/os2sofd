package dk.digitalidentity.sofd.controller.mvc.xls;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.View;

import dk.digitalidentity.sofd.dao.model.OrgUnit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class OrgUnitsXlsxView implements View {
	private static final String CONTENT_TYPE = "application/ms-excel";
	private String filename;
	private int rowCount = 1;

	@Override
	public String getContentType() {
		return CONTENT_TYPE;
	}
	
	public OrgUnitsXlsxView(String filename) {
		this.filename = filename;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		List<OrgUnit> orgUnits = (List<OrgUnit>) model.get("orgUnits");
		ResourceBundleMessageSource messageSource = (ResourceBundleMessageSource) model.get("messagesBundle");
		Locale locale = (Locale) model.get("locale");

		response.setContentType(getContentType());
		response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

		try (Workbook workbook = new DisposableSXSSFWorkbook()) {
			// create excel xls sheet
			Sheet sheet = workbook.createSheet(messageSource.getMessage("xls.orgunits.sheetname", null, locale));
	
			// required to support auto-formatting
		    ((SXSSFSheet) sheet).trackAllColumnsForAutoSizing();

			// create header row
			createHeader(workbook, sheet, messageSource, locale);
	
			OrgUnit rootOU = orgUnits.stream().filter(ou -> ou.getParent() == null).findAny().orElse(null);
			if (rootOU == null) {
				format(sheet);
				return;
			}
	
			makeTree(rootOU, sheet, 0);
	
			format(sheet);
			
			workbook.write(response.getOutputStream());
		}
	}

	private void makeTree(OrgUnit orgUnit, Sheet sheet, int indent) {
		Row courseRow = sheet.createRow(rowCount++);
		
		courseRow.createCell(0).setCellValue(orgUnit.getUuid());
		for(int i = 0; i < indent; i++) {
			courseRow.createCell(i+1);
		}
		courseRow.createCell(indent+1).setCellValue(orgUnit.getName());
		
		int newIndent = indent+1;
		List<OrgUnit> sortedChildren = orgUnit.getChildren().stream().filter(o -> !o.isDeleted()).sorted((o1,o2)-> o1.getName().compareTo(o2.getName())).collect(Collectors.toList());
		for (OrgUnit child : sortedChildren) {
			makeTree(child, sheet, newIndent);
		}
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
		sheet.autoSizeColumn(9);
	}

	private void createHeader(Workbook workbook, Sheet sheet, ResourceBundleMessageSource messageSource, Locale locale) {
		String hUuid = messageSource.getMessage("xls.orgunits.ou.uuid", null, locale);

		Font headerFont = workbook.createFont();
		headerFont.setBold(true);

		CellStyle headerStyle = workbook.createCellStyle();
		headerStyle.setFont(headerFont);

		Row header = sheet.createRow(0);
		createCell(header, 0, hUuid, headerStyle);
	}

	private static void createCell(Row header, int column, String value, CellStyle style) {
		Cell cell = header.createCell(column);
		cell.setCellValue(value);
		cell.setCellStyle(style);
	}
}
