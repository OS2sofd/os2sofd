package dk.digitalidentity.sofd.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderDeactivateAndDeleteRule;
import dk.digitalidentity.sofd.security.RequireDaoWriteAccess;
import dk.digitalidentity.sofd.service.AffiliationService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.PersonService;
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
public class AffiliationApiController {

    @Autowired
    private PersonService personService;

    @Autowired
    private AffiliationService affiliationService;

    @Autowired
    private OrgUnitService orgUnitService;

    @Autowired
    private ObjectMapper mapper;

    @PostMapping("/api/affiliation/{affiliationUuid}/alternativeOrgUnit/{orgUnitUuid}")
    public ResponseEntity<String> setAlternativeOrgUnit(@PathVariable("affiliationUuid") String affiliationUuid,
            @PathVariable("orgUnitUuid") String orgUnitUuid) {
        var affiliation = affiliationService.findByUuid(affiliationUuid);
        if (affiliation == null) {
            return new ResponseEntity<String>("Affiliation with uuid " + affiliationUuid + " not found.", HttpStatus.NOT_FOUND);
        }
        var orgUnit = orgUnitService.getByUuid(orgUnitUuid);
        if (orgUnit == null) {
            return new ResponseEntity<String>("Orgunit with uuid " + orgUnitUuid + " not found.", HttpStatus.NOT_FOUND);
        }

        affiliation.setAlternativeOrgUnit(orgUnit);
        personService.save(affiliation.getPerson());

        return new ResponseEntity<String>(HttpStatus.OK);
    }

    @DeleteMapping("/api/affiliation/{affiliationUuid}/alternativeOrgUnit")
    public ResponseEntity<String> deleteAlternativeOrgUnit(@PathVariable("affiliationUuid") String affiliationUuid) {
        var affiliation = affiliationService.findByUuid(affiliationUuid);
        if (affiliation == null) {
            return new ResponseEntity<String>("Affiliation with uuid " + affiliationUuid + " not found.", HttpStatus.NOT_FOUND);
        }

        affiliation.setAlternativeOrgUnit(null);
        personService.save(affiliation.getPerson());

        return new ResponseEntity<String>(HttpStatus.OK);
    }

    @PostMapping("/api/affiliation/{affiliationUuid}/positionDisplayname")
    public ResponseEntity<String> setPositionDisplayname(@PathVariable("affiliationUuid") String affiliationUuid, @RequestBody String positionDisplayName) {
        var affiliation = affiliationService.findByUuid(affiliationUuid);
        if (affiliation == null) {
            return new ResponseEntity<String>("Affiliation with uuid " + affiliationUuid + " not found.", HttpStatus.NOT_FOUND);
        }

        affiliation.setPositionDisplayName(positionDisplayName);
        personService.save(affiliation.getPerson());

        return new ResponseEntity<String>(HttpStatus.OK);
    }

    @DeleteMapping("/api/affiliation/{affiliationUuid}/positionDisplayname")
    public ResponseEntity<String> deletePositionDisplayname(@PathVariable("affiliationUuid") String affiliationUuid) {
        var affiliation = affiliationService.findByUuid(affiliationUuid);
        if (affiliation == null) {
            return new ResponseEntity<String>("Affiliation with uuid " + affiliationUuid + " not found.", HttpStatus.NOT_FOUND);
        }

        affiliation.setPositionDisplayName(null);
        personService.save(affiliation.getPerson());

        return new ResponseEntity<String>(HttpStatus.OK);
    }

    @GetMapping("/api/affiliation/{affiliationUuid}/localextensions")
    public ResponseEntity<Map<String,String>> getLocalExtensions(@PathVariable("affiliationUuid") String affiliationUuid) {
        var affiliation = affiliationService.findByUuid(affiliationUuid);
        if (affiliation == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        var localExtensions = stringToMap(affiliation.getLocalExtensions());
        return new ResponseEntity<>(localExtensions, HttpStatus.OK);
        //return new ResponseEntity<>(affiliation.getLocalExtensions(), HttpStatus.OK);
    }

    @PostMapping("/api/affiliation/{affiliationUuid}/localextensions")
    public ResponseEntity<String> getLocalExtensions(@PathVariable("affiliationUuid") String affiliationUuid, @RequestBody(required = false) Map<String,String> localExtensions) {
        var person  = personService.findByAffiliationUuid(affiliationUuid);
        if (person == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        var affiliation = person.getAffiliations().stream().filter(a -> a.getUuid().equalsIgnoreCase(affiliationUuid)).findFirst().get();

        try {;
            affiliation.setLocalExtensions(mapToString(localExtensions));
            personService.save(person);
            return new ResponseEntity<>(HttpStatus.OK);

        } catch (Exception e) {
            return new ResponseEntity<>("Invalid JSON format", HttpStatus.BAD_REQUEST);
        }
    }

    public record DeactivateAndDeleteRuleDto(AccountOrderDeactivateAndDeleteRule deactivateAndDeleteRule) {}
    @PostMapping("/api/affiliation/{affiliationUuid}/deactivateAndDeleteRule")
    public ResponseEntity<String> setDeactivateAndDeleteRule(@PathVariable("affiliationUuid") String affiliationUuid, @RequestBody DeactivateAndDeleteRuleDto deactivateAndDeleteRuleDto) {
        var person = personService.findByAffiliationUuid(affiliationUuid);
        if (person == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        var affiliation = person.getAffiliations().stream().filter(a -> a.getUuid().equalsIgnoreCase(affiliationUuid)).findFirst().get();
        
        if( deactivateAndDeleteRuleDto.deactivateAndDeleteRule == AccountOrderDeactivateAndDeleteRule.DEACTIVATE_AND_DELETE_IF_HOURLY_PAID )
        {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        affiliation.setDeactivateAndDeleteRule(deactivateAndDeleteRuleDto.deactivateAndDeleteRule);
        personService.save(person);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    //getDeactivateAndDeleteRule

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

    @SuppressWarnings("unchecked")
    protected Map<String, String> stringToMap(String localExtension) {
        if (!StringUtils.hasLength(localExtension)) {
            return null;
        }

        try {
            Map<String, String> map = mapper.readValue(localExtension, Map.class);

            // return sorted
            return new TreeMap<>(map);
        }
        catch (Exception ex) {
            log.error("Failed to convert string to map", ex);

            return null;
        }
    }

}