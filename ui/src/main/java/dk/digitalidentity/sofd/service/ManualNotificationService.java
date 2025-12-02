package dk.digitalidentity.sofd.service;

import dk.digitalidentity.sofd.controller.mvc.dto.ManualNotificationDTO;
import dk.digitalidentity.sofd.dao.ManualNotificationDAO;
import dk.digitalidentity.sofd.dao.model.ManualNotification;
import dk.digitalidentity.sofd.dao.model.enums.ManualNotificationFrequency;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ManualNotificationService {
    private final ManualNotificationDAO manualNotificationDAO;
    private final MessageSource messageSource;

    public void delete(ManualNotification manualNotification) { manualNotificationDAO.delete(manualNotification); }

    public List<ManualNotification> findAll() { return manualNotificationDAO.findAll(); }

    //the "or before" are added if a day should be skipped for any reason, then it will run the day after
    public List<ManualNotification> findAllActiveWithNextDateTodayOrBefore() {
        return manualNotificationDAO.findByActiveTrueAndNextDateLessThanEqual(LocalDate.now());
    }

    public Optional<ManualNotification> findById(long id) { return manualNotificationDAO.findById(id); }

    public ManualNotification save(ManualNotification manualNotification) { return manualNotificationDAO.save(manualNotification); }

    public void saveAll(List<ManualNotification> manualNotifications) { manualNotificationDAO.saveAll(manualNotifications); }

    public ManualNotification updateNextDate(ManualNotification manualNotification) {
        manualNotification.setLastRun(LocalDate.now());
        if(manualNotification.getFrequency().equals(ManualNotificationFrequency.ONCE)) {
            manualNotification.setActive(false);
        }
        else if (manualNotification.getFrequency().equals(ManualNotificationFrequency.DAILY)) {
            manualNotification.setActive(true);
            //the minimum nextDate value is 1
            manualNotification.setNextDate(LocalDate.now().plusDays(manualNotification.getFrequencyQualifier() >= 1 ? manualNotification.getFrequencyQualifier() : 1));
        }
        else if (manualNotification.getFrequency().equals(ManualNotificationFrequency.MONTHLY)) {
            manualNotification.setActive(true);
            //the minimum nextDate value is 1
            manualNotification.setNextDate(LocalDate.now().plusMonths(manualNotification.getFrequencyQualifier() >= 1 ? manualNotification.getFrequencyQualifier() : 1));
        }
        return manualNotification;
    }

    public ManualNotificationDTO toDTO(ManualNotification manualNotification) {
        return new ManualNotificationDTO(
                manualNotification.getId(),
                manualNotification.getTitle(),
                manualNotification.getDetails(),
                generateFrequencyString(manualNotification),
                manualNotification.getFirstDate(),
                manualNotification.getNextDate(),
                manualNotification.getLastRun(),
                manualNotification.isActive()
        );
    }

    private String generateFrequencyString(ManualNotification manualNotification) {
        if (manualNotification.getFrequency() == null) {
            return messageSource.getMessage(ManualNotificationFrequency.ONCE.getMessage(), null, LocaleContextHolder.getLocale()) + ".";
        }
        else if (manualNotification.getFrequency() == ManualNotificationFrequency.ONCE || manualNotification.getFrequencyQualifier() <= 1) {
            return messageSource.getMessage(manualNotification.getFrequency().getMessage(), null, LocaleContextHolder.getLocale()) + ".";
        }
        return "Hver " + manualNotification.getFrequencyQualifier() + ". " + messageSource.getMessage(manualNotification.getFrequency().getUnit(), null, LocaleContextHolder.getLocale()) + ".";
    }
}
