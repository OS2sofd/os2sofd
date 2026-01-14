package dk.digitalidentity.sofd.service.classification.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateClassificationRequest {
    @NotBlank(message = "Name is required")
    private String name;
}