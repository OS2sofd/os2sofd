package dk.digitalidentity.sofd.controller.mvc.dto;

import dk.digitalidentity.sofd.dao.model.ManualNotification;
import dk.digitalidentity.sofd.dao.model.Notification;
import dk.digitalidentity.sofd.dao.model.enums.ManualNotificationFrequency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class ManualNotificationDTO {
    private long id;
    private String title;
    private String detail;
    private String frequency;
    private LocalDate firstDate;
    private LocalDate nextDate;
    private boolean active;

    public static ManualNotificationDTO toDTO(ManualNotification manualNotification) {
        return new ManualNotificationDTO(
                manualNotification.getId(),
                manualNotification.getTitle(),
                manualNotification.getDetails(),
                "Hver " + manualNotification.getFrequencyQualifier() + ". " + manualNotification.getFrequency().getMessage(),
                manualNotification.getFirstDate(),
                manualNotification.getNextDate(),
                manualNotification.isActive()
        );
    }
}
