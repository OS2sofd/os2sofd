package dk.digitalidentity.sofd.dao;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.Notification;
import dk.digitalidentity.sofd.dao.model.enums.NotificationType;

public interface NotificationDao extends CrudRepository<Notification, Long> {
	List<Notification> findAll();

	Notification findById(long id);

	long countByActiveTrue();

	List<Notification> findAllByNotificationType(NotificationType type);

	void deleteByNotificationType(NotificationType type);
}
