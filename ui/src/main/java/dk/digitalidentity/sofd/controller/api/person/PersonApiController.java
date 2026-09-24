package dk.digitalidentity.sofd.controller.api.person;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import dk.digitalidentity.sofd.security.RequireDaoWriteAccess;
import dk.digitalidentity.sofd.service.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
@Tag(name = "Person", description = "Person management API")
@RequireDaoWriteAccess
@Slf4j
public class PersonApiController {

    private final PersonService personService;
    private final ObjectMapper mapper;

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

    @Operation(summary = "Get local extensions for a person",
            description = "Retrieves all local extensions configured for a specific person")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved local extensions"),
            @ApiResponse(responseCode = "404", description = "Person not found")
    })
    @GetMapping("/{uuid}/localextensions")
    public ResponseEntity<Map<String, String>> getLocalExtensions(
            @Parameter(description = "UUID of the person")
            @PathVariable String uuid) {
        var person = personService.getByUuid(uuid);
        if (person == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        var localExtensions = stringToMap(person.getLocalExtensions());
        return new ResponseEntity<>(localExtensions, HttpStatus.OK);
    }

    @Operation(summary = "Set local extensions for a person",
            description = "Updates or creates local extensions for a specific person")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Local extensions updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid JSON format"),
            @ApiResponse(responseCode = "404", description = "Person not found")
    })
    @PostMapping("/{uuid}/localextensions")
    public ResponseEntity<String> setLocalExtensions(
            @Parameter(description = "UUID of the person")
            @PathVariable String uuid,
            @RequestBody(required = false) Map<String, String> localExtensions) {
        var person = personService.getByUuid(uuid);
        if (person == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        try {
            person.setLocalExtensions(mapToString(localExtensions));
            personService.save(person);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("Invalid JSON format", HttpStatus.BAD_REQUEST);
        }
    }

    private String mapToString(Map<String, String> map) throws Exception {
        if (map == null) {
            return null;
        }

        try {
            return mapper.writeValueAsString(new TreeMap<>(map));
        }
        catch (Exception ex) {
            throw new Exception("Failed to convert map to string", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> stringToMap(String localExtensions) {
        if (!StringUtils.hasLength(localExtensions)) {
            return null;
        }

        try {
            Map<String, String> map = mapper.readValue(localExtensions, Map.class);

            // return sorted
            return new TreeMap<>(map);
        }
        catch (Exception ex) {
            log.error("Failed to convert string to map", ex);

            return null;
        }
    }
}