package dk.digitalidentity.sofd.controller.api;

import dk.digitalidentity.sofd.security.RequireApiWriteAccess;
import dk.digitalidentity.sofd.service.CprUpdateService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequireApiWriteAccess
public class CprController {

    @Autowired
    private CprUpdateService cprUpdateService;

    @PostMapping("/api/cpr/fullSync")
    public ResponseEntity<?> triggerFullSync() {
        log.info("Full cpr-update triggered through API");
        cprUpdateService.updateAllPersons();
        return ResponseEntity.ok().build();
    }

}
