package dk.digitalidentity.sofd.dao.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import dk.digitalidentity.sofd.dao.model.enums.ManualNotificationFrequency;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
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
    private boolean active;

    @Column
    private String title;

    @Column
    private String details;

    @Column
    @Enumerated(EnumType.STRING)
    private ManualNotificationFrequency frequency;

    @Column
    private int frequencyQualifier;

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
