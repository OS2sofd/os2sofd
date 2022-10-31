package dk.digitalidentity.sofd.controller.mvc.datatables.dao;

import org.springframework.data.jpa.datatables.repository.DataTablesRepository;

import dk.digitalidentity.sofd.controller.mvc.datatables.dao.model.AuditLogView;

public interface AuditLogViewDatatableDao extends DataTablesRepository<AuditLogView, Long> {

}