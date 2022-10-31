package dk.digitalidentity.sofd.controller.api;

import dk.digitalidentity.sofd.security.RequireDaoWriteAccess;
import dk.digitalidentity.sofd.service.PersonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequireDaoWriteAccess
@RestController
public class UserApiController {

    @Autowired
    private PersonService personService;

    @DeleteMapping("/api/user/deleteUserByADMasterId/{masterId}")
    public ResponseEntity<String> deleteUserByADMasterId(@PathVariable("masterId") String masterId) {
        personService.deleteUserByADMasterId(masterId);
        return new ResponseEntity<String>(HttpStatus.OK);
    }
}