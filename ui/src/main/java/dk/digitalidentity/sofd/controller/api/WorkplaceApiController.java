package dk.digitalidentity.sofd.controller.api;

import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.security.RequireApiWriteAccess;
import dk.digitalidentity.sofd.service.PersonService;
import dk.digitalidentity.sofd.service.WorkplaceService;
import dk.digitalidentity.sofd.service.model.WorkplacePeriod;

@RestController
@RequireApiWriteAccess
public class WorkplaceApiController {

	@Autowired
	private WorkplaceService workplaceService;

    @Autowired
    private PersonService personService;

    @Autowired
	private SofdConfiguration sofdConfiguration;

	public record WorkplacePeriodDTO(String affiliationMasterId, List<WorkplacePeriod> workplacePeriods) { }
	
	@GetMapping("/api/workplace/all")
	public ResponseEntity<List<WorkplacePeriodDTO>> getAll() {
		var workplaces = workplaceService.findAll();
		var result = new ArrayList<WorkplacePeriodDTO>();
		var workPlacesPerAffiliation = workplaces.stream().collect(groupingBy(w -> w.getAffiliation().getMasterId()));
		for( var masterId : workPlacesPerAffiliation.keySet() ) {
			var workplacePeriods = workPlacesPerAffiliation.get(masterId).stream().map(w -> new WorkplacePeriod(w.getOrgUnit().getUuid(),w.getStartDate(),w.getStopDate())).toList();
			var workplacePeriodDto = new WorkplacePeriodDTO(masterId,workplacePeriods);
			result.add(workplacePeriodDto);
		}
		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@PostMapping("/api/workplace/setForAffiliation/{affiliationMasterId}")
	public ResponseEntity<?> setForAffiliation(@PathVariable("affiliationMasterId") String masterId, @RequestBody List<WorkplacePeriod> workplacePeriods) {
		var wagesMaster = sofdConfiguration.getModules().getLos().getPrimeAffiliationMaster();
		var person = personService.findByAffiliationMasterAndMasterId(wagesMaster, masterId);
		if( person == null ) {
			return new ResponseEntity<String>("Person with wages Affiliation with affiliationMasterId " + masterId + " not found", HttpStatus.NOT_FOUND);
		}

		var matchingAffiliation = person.getAffiliations().stream().filter(a -> a.getMaster().equalsIgnoreCase(wagesMaster) && a.getMasterId().equalsIgnoreCase(masterId)).findFirst().get();
		if( workplaceService.setWorkplaces(matchingAffiliation, workplacePeriods) ) {
			personService.save(person);
		}
		else {
			return new ResponseEntity<String>("Invalid workplace periods", HttpStatus.BAD_REQUEST);
		}
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
