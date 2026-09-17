package dk.digitalidentity.sofd.controller.mvc.datatables.dao;

import org.springframework.data.jpa.datatables.repository.DataTablesRepository;

import dk.digitalidentity.sofd.controller.mvc.datatables.dao.model.AccountOrderView;

public interface AccountOrderViewDatatableDao extends DataTablesRepository<AccountOrderView, Long> {

}
