package dk.digitalidentity.sofd.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.security.RequireApiWriteAccess;
import dk.digitalidentity.sofd.service.AuthorizationCodeService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequireApiWriteAccess
public class AuthorizationCodeApiController {

	@Autowired
	private AuthorizationCodeService authorizationCodeService;

	@PostMapping("/api/authcode/fullSync")
	public ResponseEntity<?> triggerFullSync() {		
		log.info("Full authcode update");

		authorizationCodeService.syncAll(true);

		return ResponseEntity.ok().build();
	}
	
	@PostMapping("/api/authcode/deltaSync")
	public ResponseEntity<?> triggerDeltaSync() {		
		log.info("Delta authcode update");

		authorizationCodeService.syncAll(false);

		return ResponseEntity.ok().build();
	}
}
