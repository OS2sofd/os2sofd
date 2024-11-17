package dk.digitalidentity.sofd.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.digitalidentity.sofd.security.RequireDaoWriteAccess;
import dk.digitalidentity.sofd.service.OrgUnitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
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
public class OrgUnitApiController {

    @Autowired
    private OrgUnitService orgUnitService;

    @Autowired
    private ObjectMapper mapper;


    @GetMapping("/api/orgUnit/{orgUnitUuid}/localextensions")
    public ResponseEntity<Map<String,String>> getLocalExtensions(@PathVariable("orgUnitUuid") String orgUnitUuid) {
        var orgUnit = orgUnitService.getByUuid(orgUnitUuid);
        if (orgUnit == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        var localExtensions = stringToMap(orgUnit.getLocalExtensions());
        return new ResponseEntity<>(localExtensions, HttpStatus.OK);
    }

    @PostMapping("/api/orgUnit/{orgUnitUuid}/localextensions")
    public ResponseEntity<String> setLocalExtensions(@PathVariable("orgUnitUuid") String orgUnitUuid, @RequestBody(required = false) Map<String,String> localExtensions) {
        var orgunit = orgUnitService.getByUuid(orgUnitUuid);
        if (orgunit == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        try {;
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
        }
        catch (Exception ex) {
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
        }
        catch (Exception ex) {
            log.error("Failed to convert string to map", ex);

            return null;
        }
    }



}