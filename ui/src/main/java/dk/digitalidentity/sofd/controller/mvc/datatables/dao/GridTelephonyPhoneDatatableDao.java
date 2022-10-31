package dk.digitalidentity.sofd.controller.mvc.datatables.dao;

import org.springframework.data.jpa.datatables.repository.DataTablesRepository;

import dk.digitalidentity.sofd.controller.mvc.datatables.dao.model.GridTelephonyPhone;

public interface GridTelephonyPhoneDatatableDao extends DataTablesRepository<GridTelephonyPhone, Long> {

}