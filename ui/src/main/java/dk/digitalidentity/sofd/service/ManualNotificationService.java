package dk.digitalidentity.sofd.service;

import dk.digitalidentity.sofd.dao.ManualNotificationDAO;
import dk.digitalidentity.sofd.dao.model.ManualNotification;
import dk.digitalidentity.sofd.dao.model.enums.ManualNotificationFrequency;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ManualNotificationService {
    private final ManualNotificationDAO manualNotificationDAO;

    public void delete(ManualNotification manualNotification) { manualNotificationDAO.delete(manualNotification); }

    public List<ManualNotification> findAll() { return manualNotificationDAO.findAll(); }

    public List<ManualNotification> findAllActiveWithNextDateToday() {
        return manualNotificationDAO.findByActiveTrueAndNextDate(LocalDate.now());
    }

    public ManualNotification findById(long id) { return manualNotificationDAO.findById(id); }

    public ManualNotification save(ManualNotification manualNotification) { return manualNotificationDAO.save(manualNotification); }

    public ManualNotification updateNextDateAndSave(ManualNotification manualNotification) {
        if(manualNotification.getFrequency().equals(ManualNotificationFrequency.ONCE)) {
            manualNotification.setActive(false);
        }
        else if (manualNotification.getFrequency().equals(ManualNotificationFrequency.DAILY)) {
            manualNotification.setActive(true);
            manualNotification.setNextDate(LocalDate.now().plusDays(manualNotification.getFrequencyQualifier()));
        }
        else if (manualNotification.getFrequency().equals(ManualNotificationFrequency.MONTHLY)) {
            manualNotification.setActive(true);
            manualNotification.setNextDate(LocalDate.now().plusMonths(manualNotification.getFrequencyQualifier()));
        }
        return manualNotificationDAO.save(manualNotification);
    }
}
