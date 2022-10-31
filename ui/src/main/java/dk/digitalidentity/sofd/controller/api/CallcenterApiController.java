package dk.digitalidentity.sofd.controller.api;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.dao.model.view.Callcenter;
import dk.digitalidentity.sofd.security.RequireLimitedReadAccess;
import dk.digitalidentity.sofd.service.CallcenterService;

@RequireLimitedReadAccess
@RestController
public class CallcenterApiController {

    @Autowired
    private CallcenterService callcenterService;

    @GetMapping("/api/phone/search")
    public ResponseEntity<?> search(@RequestParam(value = "searchTerm") String searchTerm) {
        if (searchTerm.length() < 2) {
        	return new ResponseEntity<>(new ArrayList<Callcenter>(), HttpStatus.OK);
        }

        String[] searchTerms = searchTerm.split(" ");
        List<Callcenter> searchResults = callcenterService.getBySearch(searchTerms);

        return new ResponseEntity<>(searchResults, HttpStatus.OK);
    }
}
