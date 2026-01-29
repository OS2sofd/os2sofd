package dk.digitalidentity.sofd.controller.mvc.xls;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.View;

import dk.digitalidentity.sofd.dao.model.AccountOrderApproved;
import dk.digitalidentity.sofd.service.AccountOrderApprovedService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class AccountOrderApprovalReportXlsView implements View {
	private static final String CONTENT_TYPE = "application/ms-excel";

	@Override
	public String getContentType() {
		return CONTENT_TYPE;
	}

	@Override
	public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		Locale locale = (Locale) model.get("locale");
		ResourceBundleMessageSource messageSource = (ResourceBundleMessageSource) model.get("messagesBundle");
		AccountOrderApprovedService accountOrderApprovedService = (AccountOrderApprovedService) model.get("accountOrderApprovedService");
		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

		List<AccountOrderApproved> approvals = accountOrderApprovedService.findAll();

		response.setContentType(getContentType());
		response.setHeader("Content-Disposition", "attachment; filename=\"rapport.xlsx\"");

		try (Workbook workbook = new DisposableSXSSFWorkbook()) {
	
			// create excel xls sheet
			Sheet sheet = workbook.createSheet(messageSource.getMessage("xls.genericreport.sheetname", null, locale));
	
			// create header row
			createHeader(workbook, sheet, messageSource, locale);
	
			// Create data cells
			int rowCount = 1;
			for (AccountOrderApproved approval : approvals) {
				Row courseRow = sheet.createRow(rowCount++);
	
				courseRow.createCell(0).setCellValue(approval.getApprovedTts().format(dtf));
				courseRow.createCell(1).setCellValue(approval.getApproverName());
				courseRow.createCell(2).setCellValue(approval.getPersonName());
				courseRow.createCell(3).setCellValue(approval.getUserId());
			}
			
			workbook.write(response.getOutputStream());
		}
	}

	private void createHeader(Workbook workbook, Sheet sheet, ResourceBundleMessageSource messageSource, Locale locale) {
		String header1 = messageSource.getMessage("html.page.accountorderapprovals.approvalTime", null, locale);
		String header2 = messageSource.getMessage("html.page.accountorderapprovals.approver", null, locale);
		String header3 = messageSource.getMessage("html.page.accountorderapprovals.person", null, locale);
		String header4 = messageSource.getMessage("html.page.accountorderapprovals.userId", null, locale);
		
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
