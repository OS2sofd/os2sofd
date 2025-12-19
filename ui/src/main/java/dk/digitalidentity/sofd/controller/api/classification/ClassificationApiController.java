package dk.digitalidentity.sofd.controller.api.classification;

import dk.digitalidentity.sofd.security.RequireDaoWriteAccess;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.service.classification.model.CreateClassificationItemRequest;
import dk.digitalidentity.sofd.service.classification.model.CreateClassificationRequest;
import dk.digitalidentity.sofd.service.classification.model.UpdateClassificationRequest;
import dk.digitalidentity.sofd.service.classification.model.UpdateClassificationItemRequest;
import dk.digitalidentity.sofd.service.classification.ClassificationService;
import dk.digitalidentity.sofd.service.classification.model.ClassificationDTO;
import dk.digitalidentity.sofd.service.classification.model.ClassificationItemDTO;
import dk.digitalidentity.sofd.service.classification.model.ClassificationWithItemsDTO;
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
import java.util.List;

@RestController
@RequestMapping("/api/classifications")
@RequiredArgsConstructor
@Tag(name = "Classification", description = "Classification management API")
public class ClassificationApiController {

    private final ClassificationService classificationService;

    @Operation(summary = "Get all classifications", description = "Retrieves all classifications without their items")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved classifications")
    @RequireReadAccess
    @GetMapping
    public ResponseEntity<List<ClassificationDTO>> getAllClassifications() {
        List<ClassificationDTO> classifications = classificationService.getAllClassifications();
        return ResponseEntity.ok(classifications);
    }

    @Operation(summary = "Get classification by identifier", description = "Retrieves a single classification with all its items")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved classification"),
            @ApiResponse(responseCode = "404", description = "Classification not found")
    })
    @RequireReadAccess
    @GetMapping("/{identifier}")
    public ResponseEntity<ClassificationWithItemsDTO> getClassificationByIdentifier(
            @Parameter(description = "Unique identifier of the classification")
            @PathVariable String identifier) {
        ClassificationWithItemsDTO classification = classificationService.getClassificationByIdentifier(identifier);
        return ResponseEntity.ok(classification);
    }

    @Operation(summary = "Create classification", description = "Creates a new classification")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Classification created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or classification already exists")
    })
    @RequireDaoWriteAccess
    @PostMapping
    public ResponseEntity<ClassificationDTO> createClassification(
            @Valid @RequestBody CreateClassificationRequest request) {
        ClassificationDTO classification = classificationService.createClassification(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(classification);
    }

    @Operation(summary = "Update classification", description = "Updates an existing classification's name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Classification updated successfully"),
            @ApiResponse(responseCode = "404", description = "Classification not found")
    })
    @RequireDaoWriteAccess
    @PutMapping("/{identifier}")
    public ResponseEntity<ClassificationDTO> updateClassification(
            @Parameter(description = "Unique identifier of the classification")
            @PathVariable String identifier,
            @Valid @RequestBody UpdateClassificationRequest request) {
        ClassificationDTO classification = classificationService.updateClassification(identifier, request);
        return ResponseEntity.ok(classification);
    }

    @Operation(summary = "Delete classification", description = "Deletes a classification and all its items")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Classification deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Classification not found")
    })
    @RequireDaoWriteAccess
    @DeleteMapping("/{identifier}")
    public ResponseEntity<Void> deleteClassification(
            @Parameter(description = "Unique identifier of the classification")
            @PathVariable String identifier) {
        classificationService.deleteClassification(identifier);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get classification item", description = "Retrieves a single classification item")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved classification item"),
            @ApiResponse(responseCode = "404", description = "Classification or item not found")
    })
    @RequireReadAccess
    @GetMapping("/{classificationIdentifier}/items/{itemIdentifier}")
    public ResponseEntity<ClassificationItemDTO> getClassificationItem(
            @Parameter(description = "Unique identifier of the classification")
            @PathVariable String classificationIdentifier,
            @Parameter(description = "Unique identifier of the item")
            @PathVariable String itemIdentifier) {
        ClassificationItemDTO item = classificationService.getClassificationItem(
                classificationIdentifier, itemIdentifier);
        return ResponseEntity.ok(item);
    }

    @Operation(summary = "Create classification item", description = "Creates a new item within a classification")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Classification item created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input or item already exists")
    })
    @RequireDaoWriteAccess
    @PostMapping("/items")
    public ResponseEntity<ClassificationItemDTO> createClassificationItem(
            @Valid @RequestBody CreateClassificationItemRequest request) {
        ClassificationItemDTO item = classificationService.createClassificationItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @Operation(summary = "Update classification item", description = "Updates a classification item's name")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Classification item updated successfully"),
            @ApiResponse(responseCode = "404", description = "Classification or item not found")
    })
    @RequireDaoWriteAccess
    @PutMapping("/{classificationIdentifier}/items/{itemIdentifier}")
    public ResponseEntity<ClassificationItemDTO> updateClassificationItem(
            @Parameter(description = "Unique identifier of the classification")
            @PathVariable String classificationIdentifier,
            @Parameter(description = "Unique identifier of the item")
            @PathVariable String itemIdentifier,
            @Valid @RequestBody UpdateClassificationItemRequest request) {
        ClassificationItemDTO item = classificationService.updateClassificationItem(
                classificationIdentifier, itemIdentifier, request);
        return ResponseEntity.ok(item);
    }

    @Operation(summary = "Delete classification item", description = "Deletes a classification item")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Classification item deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Classification or item not found")
    })
    @RequireDaoWriteAccess
    @DeleteMapping("/{classificationIdentifier}/items/{itemIdentifier}")
    public ResponseEntity<Void> deleteClassificationItem(
            @Parameter(description = "Unique identifier of the classification")
            @PathVariable String classificationIdentifier,
            @Parameter(description = "Unique identifier of the item")
            @PathVariable String itemIdentifier) {
        classificationService.deleteClassificationItem(classificationIdentifier, itemIdentifier);
        return ResponseEntity.noContent().build();
    }
}