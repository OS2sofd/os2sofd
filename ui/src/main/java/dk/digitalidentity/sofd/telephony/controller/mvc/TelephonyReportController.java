package dk.digitalidentity.sofd.telephony.controller.mvc;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.security.RequireTelephonyAccess;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.telephony.TelephonyConstants;
import dk.digitalidentity.sofd.telephony.xls.TelephonyPersonsXlsView;

@RequireTelephonyAccess
@Controller
public class TelephonyReportController {
	
	@Autowired
	private PersonService personService;

	@Autowired
	private MessageSource messageSource;

	@GetMapping("/ui/telephony/report/stoppedEmployees")
	public ModelAndView stoppedEmployees(HttpServletResponse response, Locale loc) {
		List<Person> persons = personService.findAllDeleted().stream()
				.filter(p -> PersonService.getPhones(p).stream()
										  .anyMatch(ph -> ph.getMaster().equals(TelephonyConstants.TELEPHONY_MASTER)))
				.collect(Collectors.toList());

		Map<String, Object> model = new HashMap<>();
		model.put("persons", persons);
		model.put("messagesBundle", messageSource);
		model.put("locale", loc);

		response.setContentType("application/ms-excel");
		response.setHeader("Content-Disposition", "attachment; filename=\"stoppede.xlsx\"");

		return new ModelAndView(new TelephonyPersonsXlsView(), model);
	}

	@GetMapping("/ui/telephony/report/taxedEmployees")
	public ModelAndView taxedEmployees(HttpServletResponse response, Locale loc) {
		List<Person> persons = personService.findAllTaxed();

		Map<String, Object> model = new HashMap<>();
		model.put("persons", persons);
		model.put("messagesBundle", messageSource);
		model.put("locale", loc);

		response.setContentType("application/ms-excel");
		response.setHeader("Content-Disposition", "attachment; filename=\"beskattede.xlsx\"");

		return new ModelAndView(new TelephonyPersonsXlsView(), model);
	}
}
