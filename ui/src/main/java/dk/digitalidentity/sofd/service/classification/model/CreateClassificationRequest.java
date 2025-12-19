package dk.digitalidentity.sofd.service.classification.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateClassificationRequest {
    @NotBlank(message = "Identifier is required")
    private String identifier;

    @NotBlank(message = "Name is required")
    private String name;
}