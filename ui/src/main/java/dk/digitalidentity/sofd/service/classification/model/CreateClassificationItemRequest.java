package dk.digitalidentity.sofd.service.classification.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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