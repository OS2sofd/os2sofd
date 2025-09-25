package dk.digitalidentity.sofd.controller.mvc.xls;

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

import dk.digitalidentity.sofd.controller.mvc.xls.AccountOrderRulesXlsDto.Pair;
import dk.digitalidentity.sofd.dao.model.OrgUnitAccountOrderType;
import dk.digitalidentity.sofd.dao.model.OrgUnitAccountOrderTypePosition;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderRule;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;
import org.springframework.web.servlet.view.document.AbstractXlsxView;

public class AccountOrderRulesXlsView extends AbstractXlsxView {

	@Override
	protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception {
		AccountOrderRulesXlsDto dto = (AccountOrderRulesXlsDto) model.get("employees");
		ResourceBundleMessageSource messageSource = (ResourceBundleMessageSource) model.get("messagesBundle");
		Locale locale = (Locale) model.get("locale");
		SupportedUserTypeService supportedUserTypeService = (SupportedUserTypeService) model.get("supportedUserTypeService");

		// create excel xls sheet
		Sheet sheet = workbook.createSheet(messageSource.getMessage("xls.rules.sheetname", null, locale));

		// create header row
		createHeader(workbook, sheet, messageSource, locale);

		// Create data cells
		int rowCount = 1;
		if (dto.getRulePairs() != null) {
			for (Pair pair : dto.getRulePairs()) {
				for (OrgUnitAccountOrderType type : pair.order.getTypes()) {
					if (type.getRule().equals(AccountOrderRule.BY_POSITION_NAME)) {
						for (OrgUnitAccountOrderTypePosition position : type.getPositions()) {
							Row courseRow = sheet.createRow(rowCount++);
	
							courseRow.createCell(0).setCellValue(pair.orgUnit.getUuid());
							courseRow.createCell(1).setCellValue(pair.orgUnit.getName());
							courseRow.createCell(2).setCellValue(supportedUserTypeService.getPrettyName(type.getUserType()));
							courseRow.createCell(3).setCellValue(position.getPositionName());
							courseRow.createCell(4).setCellValue(messageSource.getMessage(position.getRule().getMessage(), null, locale));
						}
					}
					else {
						Row courseRow = sheet.createRow(rowCount++);
	
						courseRow.createCell(0).setCellValue(pair.orgUnit.getUuid());
						courseRow.createCell(1).setCellValue(pair.orgUnit.getName());
						courseRow.createCell(2).setCellValue(supportedUserTypeService.getPrettyName(type.getUserType()));
						courseRow.createCell(3).setCellValue("-");
						courseRow.createCell(4).setCellValue(messageSource.getMessage(type.getRule().getMessage(), null, locale));
					}
				}
			}
		}

		format(sheet);
	}

	private void format(Sheet sheet) {
		sheet.autoSizeColumn(0);
		sheet.autoSizeColumn(1);
		sheet.autoSizeColumn(2);
		sheet.autoSizeColumn(3);
		sheet.autoSizeColumn(4);
	}

	private void createHeader(Workbook workbook, Sheet sheet, ResourceBundleMessageSource messageSource, Locale locale) {
		String hUuid = messageSource.getMessage("xls.rules.ou.uuid", null, locale);
		String hOrgUnit = messageSource.getMessage("xls.rules.ou.name", null, locale);
		String hUserType = messageSource.getMessage("xls.rules.usertype", null, locale);
		String hPosition = messageSource.getMessage("xls.rules.position", null, locale);
		String hRule = messageSource.getMessage("xls.rules.rule", null, locale);

		Font headerFont = workbook.createFont();
		headerFont.setBold(true);

		CellStyle headerStyle = workbook.createCellStyle();
		headerStyle.setFont(headerFont);

		Row header = sheet.createRow(0);
		createCell(header, 0, hUuid, headerStyle);
		createCell(header, 1, hOrgUnit, headerStyle);
		createCell(header, 2, hUserType, headerStyle);
		createCell(header, 3, hPosition, headerStyle);
		createCell(header, 4, hRule, headerStyle);
	}

	private static void createCell(Row header, int column, String value, CellStyle style) {
		Cell cell = header.createCell(column);
		cell.setCellValue(value);
		cell.setCellStyle(style);
	}
}
