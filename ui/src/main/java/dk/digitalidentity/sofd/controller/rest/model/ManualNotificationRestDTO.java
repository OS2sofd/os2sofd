package dk.digitalidentity.sofd.controller.rest.model;

import dk.digitalidentity.sofd.dao.model.ManualNotification;
import dk.digitalidentity.sofd.dao.model.enums.ManualNotificationFrequency;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@AllArgsConstructor
@Data
@NoArgsConstructor
public class ManualNotificationRestDTO {
    private long id;
    private boolean active;
    private String title;
    private String details;
    @DateTimeFormat(pattern = "yyyy-MM-dd") private LocalDate nextDate;
    private ManualNotificationFrequency frequency;
    private int frequencyQualifier;

    public static ManualNotificationRestDTO toDTO(ManualNotification manualNotification) {
        return new ManualNotificationRestDTO(
           manualNotification.getId(),
           manualNotification.isActive(),
           manualNotification.getTitle(),
           manualNotification.getDetails(),
           manualNotification.getNextDate(),
           manualNotification.getFrequency(),
           manualNotification.getFrequencyQualifier()
        );
    }
}
