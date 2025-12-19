package dk.digitalidentity.sofd.service.classification.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationDTO {
    private String identifier;
    private String name;
}