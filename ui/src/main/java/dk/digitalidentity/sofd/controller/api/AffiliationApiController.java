package dk.digitalidentity.sofd.controller.api;

import dk.digitalidentity.sofd.security.RequireDaoWriteAccess;
import dk.digitalidentity.sofd.service.AffiliationService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequireDaoWriteAccess
@RestController
public class AffiliationApiController {

    @Autowired
    private PersonService personService;

    @Autowired
    private AffiliationService affiliationService;

    @Autowired
    private OrgUnitService orgUnitService;

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

}