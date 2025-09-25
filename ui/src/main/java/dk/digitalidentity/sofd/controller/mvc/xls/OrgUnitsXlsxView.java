package dk.digitalidentity.sofd.controller.mvc.xls;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.context.support.ResourceBundleMessageSource;

import dk.digitalidentity.sofd.dao.model.OrgUnit;
import org.springframework.web.servlet.view.document.AbstractXlsxView;

public class OrgUnitsXlsxView extends AbstractXlsxView {

	private int rowCount = 1;

	@Override
	protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception {
		@SuppressWarnings("unchecked")
		List<OrgUnit> orgUnits = (List<OrgUnit>) model.get("orgUnits");
		ResourceBundleMessageSource messageSource = (ResourceBundleMessageSource) model.get("messagesBundle");
		Locale locale = (Locale) model.get("locale");

		// create excel xls sheet
		Sheet sheet = workbook.createSheet(messageSource.getMessage("xls.orgunits.sheetname", null, locale));

		// create header row
		createHeader(workbook, sheet, messageSource, locale);

		OrgUnit rootOU = orgUnits.stream().filter(ou -> ou.getParent() == null).findAny().orElse(null);
		if (rootOU == null) {
			format(sheet);
			return;
		}

		makeTree(rootOU, sheet, 0);

		format(sheet);
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
