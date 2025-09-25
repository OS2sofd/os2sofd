package dk.digitalidentity.sofd.controller.mvc.datatables.dao;

import dk.digitalidentity.sofd.controller.mvc.datatables.dao.model.GridStudent;
import org.springframework.data.jpa.datatables.repository.DataTablesRepository;

public interface GridStudentDatatableDao extends DataTablesRepository<GridStudent, String> {

}