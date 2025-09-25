package dk.digitalidentity.sofd.controller.mvc.datatables.dao;

import org.springframework.data.jpa.datatables.repository.DataTablesRepository;

import dk.digitalidentity.sofd.dao.model.NotificationView;

public interface NotificationDatatableDao extends DataTablesRepository<NotificationView, Long> {

}
