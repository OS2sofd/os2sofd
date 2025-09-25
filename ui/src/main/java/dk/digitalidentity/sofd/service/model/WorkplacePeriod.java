package dk.digitalidentity.sofd.service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
public class WorkplacePeriod {
    private String orgUnitUuid;
    private LocalDate startDate;
    private LocalDate stopDate;
}
