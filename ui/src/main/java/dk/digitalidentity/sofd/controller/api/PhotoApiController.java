package dk.digitalidentity.sofd.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.security.RequireDaoWriteAccess;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.PhotoService;

@RequireDaoWriteAccess
@RestController
public class PhotoApiController {

    @Autowired
    private PersonService personService;

    @Autowired
    private PhotoService photoService;

    @PostMapping("/api/photo/{personUuid}")
    public ResponseEntity<String> Post(@PathVariable("personUuid") String personUuid, @RequestBody byte[] data) {
        if (personService.getByUuid(personUuid) == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        photoService.save(personUuid, data);
        return new ResponseEntity<String>(HttpStatus.OK);
    }

    @DeleteMapping("/api/photo/{personUuid}")
    public ResponseEntity<String> Delete(@PathVariable("personUuid") String personUuid) {
        if (personService.getByUuid(personUuid) == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        photoService.delete(personUuid);
        return new ResponseEntity<String>(HttpStatus.OK);
    }
}