package dk.digitalidentity.sofd.controller.rest;

import java.util.List;

import dk.digitalidentity.sofd.security.RequireReadAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.controller.mvc.dto.PUnitDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.PnrSearchResultDTO;
import dk.digitalidentity.sofd.service.CvrService;

@RequireReadAccess
@RestController
public class CvrRestController {
	
	@Autowired
	private CvrService cvrService;

	@GetMapping("/rest/cvr/search")
	public ResponseEntity<?> search(@RequestParam boolean outside, @RequestParam String query) throws Exception {
		List<PnrSearchResultDTO> response = null;
		if (outside) {
			if (cvrService.validPnr(query)) {
				response = cvrService.getSearchResultByPnr(query);
			}else {
				response = cvrService.getSearchResultAllPnrsByName(query);
			}
		} else {
			if (cvrService.validPnr(query)) {
				response = cvrService.getSearchResultByOwnCvrAndPnr(query);
			}else {
				response = cvrService.getSearchResultPnrsForOwnCvrByName(query);
			}
		}
		
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
	
	@GetMapping("/rest/cvr/select")
	public ResponseEntity<?> search(@RequestParam String pnr) throws Exception {
		if (!cvrService.validPnr(pnr)) {
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}
		
		PUnitDTO response = cvrService.getPUnitByPnr(pnr);
		
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
