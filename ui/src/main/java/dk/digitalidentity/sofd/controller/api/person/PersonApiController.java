package dk.digitalidentity.sofd.controller.api.person;

import dk.digitalidentity.sofd.security.RequireDaoWriteAccess;
import dk.digitalidentity.sofd.service.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Objects;

@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
@Tag(name = "Person", description = "Person management API")
@RequireDaoWriteAccess
public class PersonApiController {

    private final PersonService personService;

    public record SetChosenNameRequest(String chosenName) {}

    @Operation(summary = "Set chosen name", description = "Sets, updates, or clears the chosen name for a person. Pass null or empty string to clear.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Chosen name updated successfully"),
            @ApiResponse(responseCode = "304", description = "Chosen name unchanged"),
            @ApiResponse(responseCode = "404", description = "Person not found")
    })
    @PutMapping("/{uuid}/chosen-name")
    public ResponseEntity<Void> setChosenName(
            @Parameter(description = "UUID of the person")
            @PathVariable String uuid,
            @Valid @RequestBody SetChosenNameRequest request) {
        var person = personService.getByUuid(uuid);
        if (person == null) {
            return ResponseEntity.notFound().build();
        }

        String normalizedChosenName = normalizeChosenName(request.chosenName());

        if (Objects.equals(person.getChosenName(), normalizedChosenName)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }
        person.setChosenName(normalizedChosenName);
        personService.save(person);
        return ResponseEntity.noContent().build();
    }

    private String normalizeChosenName(String chosenName) {
        if (chosenName == null || chosenName.isBlank()) {
            return null;
        }
        return chosenName;
    }
}