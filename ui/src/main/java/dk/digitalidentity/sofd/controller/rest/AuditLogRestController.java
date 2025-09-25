package dk.digitalidentity.sofd.controller.rest;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.controller.mvc.datatables.dao.AuditLogViewDatatableDao;
import dk.digitalidentity.sofd.controller.mvc.datatables.dao.model.AuditLogView;
import dk.digitalidentity.sofd.controller.mvc.datatables.dao.model.dto.AuditLogDTO;
import dk.digitalidentity.sofd.security.RequireReadAccess;

@RequireReadAccess
@RestController
public class AuditLogRestController {

	@Autowired
	private AuditLogViewDatatableDao auditLogDatatableDao;
	
	@Autowired
	private MessageSource messageSource;
	
	@PostMapping("/rest/auditlog/list")
	public DataTablesOutput<AuditLogDTO> list(@Valid @RequestBody DataTablesInput input, BindingResult bindingResult, @RequestHeader("show-system") boolean showSystem, Locale locale) {
		if (bindingResult.hasErrors()) {
			DataTablesOutput<AuditLogDTO> error = new DataTablesOutput<>();
			error.setError(bindingResult.toString());

			return error;
		}

		DataTablesOutput<AuditLogView> output;
		if (!showSystem) {
			//this filters out all system auditLogs
			output = auditLogDatatableDao.findAll(input, (root, query, criteriaBuilder) -> criteriaBuilder.notEqual(root.get("userId"), "system"));
		} else {
			output = auditLogDatatableDao.findAll(input);
		}

		return convertAuditLogDataTablesModelToDTO(output, locale);
	}

	private DataTablesOutput<AuditLogDTO> convertAuditLogDataTablesModelToDTO(DataTablesOutput<AuditLogView> output, Locale locale) {
		List<AuditLogDTO> dataWithMessages = output.getData().stream().map(auditlog -> new AuditLogDTO(auditlog, messageSource, locale)).collect(Collectors.toList());
		
		DataTablesOutput<AuditLogDTO> result = new DataTablesOutput<>();
		result.setData(dataWithMessages);
		result.setDraw(output.getDraw());
		result.setError(output.getError());
		result.setRecordsFiltered(output.getRecordsFiltered());
		result.setRecordsTotal(output.getRecordsTotal());

		return result;
	}
}
