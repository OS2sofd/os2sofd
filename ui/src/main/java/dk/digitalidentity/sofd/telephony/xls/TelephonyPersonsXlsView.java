package dk.digitalidentity.sofd.telephony.xls;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.context.support.ResourceBundleMessageSource;

import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.Phone;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.telephony.TelephonyConstants;
import org.springframework.web.servlet.view.document.AbstractXlsxView;

public class TelephonyPersonsXlsView extends AbstractXlsxView {

	@SuppressWarnings("unchecked")
	@Override
	protected void buildExcelDocument(Map<String, Object> model, Workbook workbook, HttpServletRequest request, HttpServletResponse response) throws Exception {
		ResourceBundleMessageSource messageSource = (ResourceBundleMessageSource) model.get("messagesBundle");
		Locale locale = (Locale) model.get("locale");
		List<Person> persons = (List<Person>) model.get("persons");

		// create excel xls sheet
		Sheet sheet = workbook.createSheet(messageSource.getMessage("xls.telephony.sheetname", null, locale));

		// create header row
		createHeader(workbook, sheet, messageSource, locale);

		// Create data cells
		int rowCount = 1;
		for (Person person : persons) {
			Row courseRow = sheet.createRow(rowCount++);

			Optional<Phone> phone = PersonService.getPhones(person).stream().filter(p -> p.getMaster().equals(TelephonyConstants.TELEPHONY_MASTER)).findFirst();
			
			courseRow.createCell(0).setCellValue(person.getUuid());
			courseRow.createCell(1).setCellValue(PersonService.getName(person));
			courseRow.createCell(2).setCellValue(person.getCpr());
			courseRow.createCell(3).setCellValue(phone.isPresent() ? phone.get().getPhoneNumber() : "<ukendt nummer>");
		}
	}

	private void createHeader(Workbook workbook, Sheet sheet, ResourceBundleMessageSource messageSource, Locale locale) {
		String hUuid = messageSource.getMessage("xls.telephony.user.uuid", null, locale);
		String hName = messageSource.getMessage("xls.telephony.user.name", null, locale);
		String nCpr = messageSource.getMessage("xls.telephony.user.cpr", null, locale);
		String hPhone = messageSource.getMessage("xls.telephony.phone", null, locale);

		Font headerFont = workbook.createFont();
		headerFont.setBold(true);

		CellStyle headerStyle = workbook.createCellStyle();
		headerStyle.setFont(headerFont);

		Row header = sheet.createRow(0);
		createCell(header, 0, hUuid, headerStyle);
		createCell(header, 1, hName, headerStyle);
		createCell(header, 2, nCpr, headerStyle);
		createCell(header, 3, hPhone, headerStyle);
	}

	private static void createCell(Row header, int column, String value, CellStyle style) {
		Cell cell = header.createCell(column);
		cell.setCellValue(value);
		cell.setCellStyle(style);
	}
}
