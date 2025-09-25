package dk.digitalidentity.sofd.controller.mvc;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import dk.digitalidentity.sofd.controller.mvc.datatables.dao.model.dto.AuditLogDTO;
import dk.digitalidentity.sofd.controller.mvc.xls.AuditLogXlsView;
import dk.digitalidentity.sofd.dao.AuditLogDao;
import dk.digitalidentity.sofd.dao.model.AuditLog;
import dk.digitalidentity.sofd.security.RequireReadAccess;

@RequireReadAccess
@Controller
public class AuditLogController {
	
	@Autowired
	private MessageSource messageSource;
	
	@Autowired
	private AuditLogDao auditLogDao;

	@GetMapping(value = "/ui/auditlogs")
	public String audit(Model model) {
		return "logs/list";
	}
	
	@GetMapping(value = "/ui/auditlogs/{id}/details")
	public String auditDetails(Model model, @PathVariable long id, Locale loc) {
		AuditLog log = auditLogDao.findById(id);
		if (log == null) {
			return "redirect:/ui/auditlogs";
		}
		
		AuditLogDTO dto = new AuditLogDTO(log, messageSource, loc);
		model.addAttribute("log", dto);
		
		return "logs/view";
	}
	
	@RequestMapping(value = "/ui/auditlogs/download")
	public ModelAndView download(HttpServletResponse response, Locale loc) {
		Map<String, Object> model = new HashMap<>();
		model.put("logs", auditLogDao.findAll());
		model.put("locale", loc);
		model.put("messagesBundle", messageSource);

		response.setContentType("application/ms-excel");
		response.setHeader("Content-Disposition", "attachment; filename=\"Auditlog.xlsx\"");

		return new ModelAndView(new AuditLogXlsView(), model);
	}
}
