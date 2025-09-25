package dk.digitalidentity.sofd.controller.rest;

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.datatables.mapping.DataTablesInput;
import org.springframework.data.jpa.datatables.mapping.DataTablesOutput;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.controller.mvc.datatables.dao.GridStudentDatatableDao;
import dk.digitalidentity.sofd.controller.mvc.datatables.dao.model.GridStudent;
import dk.digitalidentity.sofd.security.RequireReadAccess;

@RequireReadAccess
@RestController
public class StudentRestController {

	@Autowired
	private GridStudentDatatableDao studentDatatableDao;

	// read access is fine here, used by datatables
	@SuppressWarnings("unchecked")
	@PostMapping("/rest/student/list")
	public DataTablesOutput<GridStudent> list(@Valid @RequestBody DataTablesInput input, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			DataTablesOutput<GridStudent> error = new DataTablesOutput<>();
			error.setError(bindingResult.toString());

			return error;
		}

		DataTablesOutput<?> result = studentDatatableDao.findAll(input);
		DataTablesOutput<GridStudent> output = new DataTablesOutput<>();
		output.setDraw(result.getDraw());
		output.setRecordsFiltered(result.getRecordsFiltered());
		output.setRecordsTotal(result.getRecordsTotal());
		output.setError(result.getError());
		output.setData((List<GridStudent>) result.getData());

		return output;
	}
}
