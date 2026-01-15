package dk.digitalidentity.sofd.dao.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import dk.digitalidentity.sofd.dao.model.enums.ManualNotificationFrequency;
import lombok.Getter;
import lombok.Setter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.LocalDate;

@Entity(name = "manual_notifications")
@Getter
@Setter
public class ManualNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column
    private long id;

    @Column
    private boolean active = true;

    @Column
    private String title;

    @Column
    private String details;

    @Column
    @Enumerated(EnumType.STRING)
    private ManualNotificationFrequency frequency = ManualNotificationFrequency.ONCE;

    @Column
    private int frequencyQualifier = 1;

    @Column
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate firstDate;

    @Column
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate nextDate;

    @Column
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate lastRun;
}
