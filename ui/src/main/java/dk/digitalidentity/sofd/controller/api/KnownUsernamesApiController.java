package dk.digitalidentity.sofd.controller.api;

import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.service.KnownUsernamesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RequireReadAccess
@RestController
public class KnownUsernamesApiController {

    @Autowired
    private KnownUsernamesService knownUsernamesService;

    private record KnownUsernameApiDTO(String Username, String Usertype) { }

    @GetMapping("/api/knownusernames")
    public ResponseEntity<?> list() {
        return new ResponseEntity<>(knownUsernamesService.findAll().stream()
                .map(ku -> new KnownUsernameApiDTO(ku.getUsername(), ku.getUserType()))
                .collect(Collectors.toList()),
                HttpStatus.OK);
    }

	@GetMapping("/api/knownusernames/{username}")
	public ResponseEntity<?> find(@PathVariable("username") String username) {
		return new ResponseEntity<>(knownUsernamesService.findByUsername(username).stream()
				.map(ku -> new KnownUsernameApiDTO(ku.getUsername(), ku.getUserType()))
				.collect(Collectors.toList()),
				HttpStatus.OK);
	}


}
