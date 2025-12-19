package dk.digitalidentity.sofd.service.classification.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClassificationWithItemsDTO {
    private String identifier;
    private String name;
    private List<ClassificationItemDTO> items;
}