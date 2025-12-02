package dk.digitalidentity.sofd.controller.mvc.dto;

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
    private String details;
    private String frequency;
    private LocalDate firstDate;
    private LocalDate nextDate;
    private LocalDate lastRun;
    private boolean active;
}
