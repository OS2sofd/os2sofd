package dk.digitalidentity.sofd.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.digitalidentity.sofd.security.RequireDaoWriteAccess;
import dk.digitalidentity.sofd.service.OrgUnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.TreeMap;

@RequireDaoWriteAccess
@RestController
@Slf4j
@Tag(name = "OrgUnit", description = "Organization Unit API")
public class OrgUnitApiController {

    @Autowired
    private OrgUnitService orgUnitService;

    @Autowired
    private ObjectMapper mapper;

    @Operation(summary = "Get display name for an organization unit",
            description = "Retrieves the display name (kaldenavn) for a specific organization unit")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved display name"),
            @ApiResponse(responseCode = "404", description = "Organization unit not found")
    })
    @GetMapping("/api/orgUnit/{orgUnitUuid}/displayName")
    public ResponseEntity<String> getDisplayName(
            @Parameter(description = "UUID of the organization unit")
            @PathVariable("orgUnitUuid") String orgUnitUuid) {
        var orgUnit = orgUnitService.getByUuid(orgUnitUuid);
        if (orgUnit == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        return new ResponseEntity<>(orgUnit.getDisplayName(), HttpStatus.OK);
    }

    @Operation(summary = "Set display name for an organization unit",
            description = "Creates or updates the display name (kaldenavn) for a specific organization unit")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Display name updated successfully"),
            @ApiResponse(responseCode = "404", description = "Organization unit not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error while saving")
    })
    @PostMapping("/api/orgUnit/{orgUnitUuid}/displayName")
    public ResponseEntity<String> setDisplayName(
            @Parameter(description = "UUID of the organization unit")
            @PathVariable("orgUnitUuid") String orgUnitUuid,
            @RequestBody String displayName) {
        var orgUnit = orgUnitService.getByUuid(orgUnitUuid);
        if (orgUnit == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        try {
            orgUnit.setDisplayName(displayName);
            orgUnitService.save(orgUnit);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to save display name for orgUnit: {}", orgUnitUuid, e);
            return new ResponseEntity<>("Failed to save display name", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Delete display name for an organization unit",
            description = "Removes the display name (kaldenavn) from a specific organization unit")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Display name deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Organization unit not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error while saving")
    })
    @DeleteMapping("/api/orgUnit/{orgUnitUuid}/displayName")
    public ResponseEntity<String> deleteDisplayName(
            @Parameter(description = "UUID of the organization unit")
            @PathVariable("orgUnitUuid") String orgUnitUuid) {
        var orgUnit = orgUnitService.getByUuid(orgUnitUuid);
        if (orgUnit == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        try {
            orgUnit.setDisplayName(null);
            orgUnitService.save(orgUnit);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (Exception e) {
            log.error("Failed to delete display name for orgUnit: {}", orgUnitUuid, e);
            return new ResponseEntity<>("Failed to delete display name", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Operation(summary = "Get local extensions for an organization unit",
            description = "Retrieves all local extensions configured for a specific organization unit")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved local extensions"),
            @ApiResponse(responseCode = "404", description = "Organization unit not found")
    })
    @GetMapping("/api/orgUnit/{orgUnitUuid}/localextensions")
    public ResponseEntity<Map<String, String>> getLocalExtensions(
            @Parameter(description = "UUID of the organization unit")
            @PathVariable("orgUnitUuid") String orgUnitUuid) {
        var orgUnit = orgUnitService.getByUuid(orgUnitUuid);
        if (orgUnit == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        var localExtensions = stringToMap(orgUnit.getLocalExtensions());
        return new ResponseEntity<>(localExtensions, HttpStatus.OK);
    }

    @Operation(summary = "Set local extensions for an organization unit",
            description = "Updates or creates local extensions for a specific organization unit")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Local extensions updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid JSON format"),
            @ApiResponse(responseCode = "404", description = "Organization unit not found")
    })
    @PostMapping("/api/orgUnit/{orgUnitUuid}/localextensions")
    public ResponseEntity<String> setLocalExtensions(
            @Parameter(description = "UUID of the organization unit")
            @PathVariable("orgUnitUuid") String orgUnitUuid,
            @RequestBody(required = false) Map<String, String> localExtensions) {
        var orgunit = orgUnitService.getByUuid(orgUnitUuid);
        if (orgunit == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        try {
            orgunit.setLocalExtensions(mapToString(localExtensions));
            orgUnitService.save(orgunit);
            return new ResponseEntity<>(HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>("Invalid JSON format", HttpStatus.BAD_REQUEST);
        }
    }

    protected String mapToString(Map<String, String> map) throws Exception {
        if (map == null) {
            return null;
        }

        try {
            return mapper.writeValueAsString(new TreeMap<>(map));
        } catch (Exception ex) {
            throw new Exception("Failed to convert map to string", ex);
        }
    }

    protected Map<String, String> stringToMap(String localExtension) {
        if (!StringUtils.hasLength(localExtension)) {
            return null;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, String> map = (Map<String, String>) mapper.readValue(localExtension, Map.class);

            // return sorted
            return new TreeMap<>(map);
        } catch (Exception ex) {
            log.error("Failed to convert string to map", ex);

            return null;
        }
    }
}