package dk.digitalidentity.sofd.dao;

import dk.digitalidentity.sofd.dao.model.ManualNotification;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ManualNotificationDAO extends CrudRepository<ManualNotification, Long> {
    List<ManualNotification> findAll();

    Optional<ManualNotification> findById(long id);

    List<ManualNotification> findByNextDate(LocalDate nextDate);

    List<ManualNotification> findByActiveTrueAndNextDate(LocalDate nextDate);

    List<ManualNotification> findByActiveTrueAndNextDateLessThanEqual(LocalDate nextDate);

}
