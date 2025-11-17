package dk.digitalidentity.sofd.dao;

import dk.digitalidentity.sofd.dao.model.ManualNotification;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;

public interface ManualNotificationDAO extends CrudRepository<ManualNotification, Long> {
    List<ManualNotification> findAll();

    ManualNotification findById(long id);

    List<ManualNotification> findByNextDate(LocalDate nextDate);

    List<ManualNotification> findByActiveTrueAndNextDate(LocalDate nextDate);
}
