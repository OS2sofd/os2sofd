package dk.digitalidentity.sofd.controller.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.security.RequireApiWriteAccess;
import dk.digitalidentity.sofd.service.xls.HistoricalReportsOnOrgUnitsService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequireApiWriteAccess
public class ReportApiController {

	@Autowired
	private HistoricalReportsOnOrgUnitsService historicalReportsOnOrgUnitsService;

	@PostMapping("/api/report/generateHistoricalOrgunitReport")
	public ResponseEntity<?> triggerHistoricalOrgunitReport() {
		log.info("Historical orgunit report generation triggered through API");

		historicalReportsOnOrgUnitsService.generateOrgunitReport();

		return ResponseEntity.ok().build();
	}
}
