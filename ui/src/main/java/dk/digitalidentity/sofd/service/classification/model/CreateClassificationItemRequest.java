package dk.digitalidentity.sofd.service.classification.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateClassificationItemRequest {
    @NotBlank(message = "Classification identifier is required")
    private String classificationIdentifier;

    @NotBlank(message = "Item identifier is required")
    private String identifier;

    private String name;  // Optional
}