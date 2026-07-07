package dk.digitalidentity.sofd.controller.api;

import dk.digitalidentity.sofd.security.RequireApiWriteAccess;
import dk.digitalidentity.sofd.service.OS2SyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequireApiWriteAccess
public class Os2SyncApiController {

    @Autowired
    private OS2SyncService os2SyncService;

    @PostMapping("/api/os2sync/synchronizeHierarchy")
    public ResponseEntity<?> triggerSynchronizeHierarchy() {
        log.info("Cleanup of OUs in FK Organisation triggered through API");
        os2SyncService.synchronizeHierarchy();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/os2sync/cleanupUsers")
    public ResponseEntity<?> triggerCleanupUsers() {
        log.info("Cleanup of users in FK Organisation triggered through API");
        os2SyncService.cleanupUsers();
        return ResponseEntity.ok().build();
    }

}
