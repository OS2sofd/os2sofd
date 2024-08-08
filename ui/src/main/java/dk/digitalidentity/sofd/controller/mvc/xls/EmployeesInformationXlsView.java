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
import org.springframework.web.servlet.view.document.AbstractXlsxStreamingView;

import dk.digitalidentity.sofd.controller.mvc.dto.EmployeeWithUsersDTO;
import dk.digitalidentity.sofd.dao.model.User;
import dk.digitalidentity.sofd.service.SupportedUserTypeService;

public class EmployeesInformationXlsView extends AbstractXlsxStreamingView {

	@SuppressWarnings("unchecked")
	@Override
	protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception {
		List<EmployeeWithUsersDTO> employeeWithUsersDTOs = (List<EmployeeWithUsersDTO>) model.get("employees");
		ResourceBundleMessageSource messageSource = (ResourceBundleMessageSource) model.get("messagesBundle");
		Locale locale = (Locale) model.get("locale");
		SupportedUserTypeService supportedUserTypeService = (SupportedUserTypeService) model.get("supportedUserTypeService");

		// create excel xls sheet
		Sheet sheet = workbook.createSheet(messageSource.getMessage("xls.employees.sheetname", null, locale));

		// create header row
		createHeader(workbook, sheet, messageSource, locale);

		// Create data cells
		int rowCount = 1;
		for (EmployeeWithUsersDTO employeeWithUsersDTO : employeeWithUsersDTOs) {
			if (employeeWithUsersDTO.getUsers().isEmpty()) {
				Row courseRow = sheet.createRow(rowCount++);
				courseRow.createCell(0).setCellValue(employeeWithUsersDTO.getUuid());
				courseRow.createCell(1).setCellValue(employeeWithUsersDTO.getName());
				courseRow.createCell(2).setCellValue(employeeWithUsersDTO.getPositionName());
				courseRow.createCell(3).setCellValue(employeeWithUsersDTO.getEmploymentTerms());
				courseRow.createCell(4).setCellValue(employeeWithUsersDTO.getStartDate());
				courseRow.createCell(5).setCellValue(employeeWithUsersDTO.getStopDate());
				courseRow.createCell(6).setCellValue("");
				courseRow.createCell(7).setCellValue("");
				courseRow.createCell(8).setCellValue(employeeWithUsersDTO.getOrgUnitUuid());
				courseRow.createCell(9).setCellValue(employeeWithUsersDTO.getOrgUnitName());
				courseRow.createCell(10).setCellValue(employeeWithUsersDTO.getManager());
				courseRow.createCell(11).setCellValue(employeeWithUsersDTO.getInternalReference());
			}
			else {
				for (User user : employeeWithUsersDTO.getUsers()) {
					Row courseRow = sheet.createRow(rowCount++);
					courseRow.createCell(0).setCellValue(employeeWithUsersDTO.getUuid());
					courseRow.createCell(1).setCellValue(employeeWithUsersDTO.getName());
					courseRow.createCell(2).setCellValue(employeeWithUsersDTO.getPositionName());
					courseRow.createCell(3).setCellValue(employeeWithUsersDTO.getEmploymentTerms());
					courseRow.createCell(4).setCellValue(employeeWithUsersDTO.getStartDate());
					courseRow.createCell(5).setCellValue(employeeWithUsersDTO.getStopDate());
					courseRow.createCell(6).setCellValue(user.getUserId());
					courseRow.createCell(7).setCellValue(supportedUserTypeService.getPrettyName(user.getUserType()));
					courseRow.createCell(8).setCellValue(employeeWithUsersDTO.getOrgUnitUuid());
					courseRow.createCell(9).setCellValue(employeeWithUsersDTO.getOrgUnitName());
					courseRow.createCell(10).setCellValue(employeeWithUsersDTO.getManager());
					courseRow.createCell(11).setCellValue(employeeWithUsersDTO.getInternalReference());
				}
			}
		}
	}

	private void createHeader(Workbook workbook, Sheet sheet, ResourceBundleMessageSource messageSource, Locale locale) {
		String hEmployeeName = messageSource.getMessage("xls.employees.header.name", null, locale);
		String hPositionName = messageSource.getMessage("xls.employees.header.position.name", null, locale);
		String hStartDate = messageSource.getMessage("xls.employees.header.start", null, locale);
		String hStopDate = messageSource.getMessage("xls.employees.header.stop", null, locale);
		String hUserName = messageSource.getMessage("xls.employees.header.username", null, locale);
		String hUserType = messageSource.getMessage("xls.employees.header.usertype", null, locale);
		String hOrgUnit = messageSource.getMessage("xls.employees.header.orgunit", null, locale);
		String hManager = messageSource.getMessage("xls.employees.header.manager", null, locale);
		String hEmployeeUuid = messageSource.getMessage("xls.employees.header.employee_uuid", null, locale);
		String hEmploymentTerms = messageSource.getMessage("xls.employees.header.employment_terms", null, locale);
		String hOrgUnitUuid = messageSource.getMessage("xls.employees.header.orgunit_uuid", null, locale);
		String hInternalReference = messageSource.getMessage("xls.employees.header.internal_reference", null, locale);

		Font headerFont = workbook.createFont();
		headerFont.setBold(true);

		CellStyle headerStyle = workbook.createCellStyle();
		headerStyle.setFont(headerFont);

		Row header = sheet.createRow(0);
		createCell(header, 0, hEmployeeUuid, headerStyle);
		createCell(header, 1, hEmployeeName, headerStyle);
		createCell(header, 2, hPositionName, headerStyle);
		createCell(header, 3, hEmploymentTerms, headerStyle);
		createCell(header, 4, hStartDate, headerStyle);
		createCell(header, 5, hStopDate, headerStyle);
		createCell(header, 6, hUserName, headerStyle);
		createCell(header, 7, hUserType, headerStyle);
		createCell(header, 8, hOrgUnitUuid, headerStyle);
		createCell(header, 9, hOrgUnit, headerStyle);
		createCell(header, 10, hManager, headerStyle);
		createCell(header, 11, hInternalReference, headerStyle);
	}

	private static void createCell(Row header, int column, String value, CellStyle style) {
		Cell cell = header.createCell(column);
		cell.setCellValue(value);
		cell.setCellStyle(style);
	}
}
