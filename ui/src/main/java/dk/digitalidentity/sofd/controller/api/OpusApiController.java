package dk.digitalidentity.sofd.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.security.RequireApiWriteAccess;
import dk.digitalidentity.sofd.service.OpusService;

@RestController
@RequireApiWriteAccess
public class OpusApiController {

	@Autowired
	private OpusService	opusService;
	
	@PostMapping("/api/opus/fullEmailSync")
	public ResponseEntity<?> triggerFullSync() {
		opusService.updateEmailForAllPersons();
		return ResponseEntity.ok().build();
	}
}
