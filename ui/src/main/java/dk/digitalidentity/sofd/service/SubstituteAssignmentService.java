package dk.digitalidentity.sofd.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.sofd.dao.SubstituteAssignmentDao;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SubstituteAssignment;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.EventType;
import dk.digitalidentity.sofd.log.AuditLogger;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SubstituteAssignmentService {

	@Autowired
	private SubstituteAssignmentDao substituteAssignmentDao;

	@Autowired
	private AuditLogger auditLogger;

	@Autowired
	private OrgUnitService orgUnitService;

	public SubstituteAssignment getById(long id) {
		return substituteAssignmentDao.findById(id);
	}

	public SubstituteAssignment save(SubstituteAssignment assignment) {
		assignment.setChanged(LocalDateTime.now());
		return substituteAssignmentDao.save(assignment);
	}

	public void delete(SubstituteAssignment assignment) {
		substituteAssignmentDao.delete(assignment);
	}

	public List<SubstituteAssignment> findAll() {
		return substituteAssignmentDao.findAll();
	}

	public List<SubstituteAssignment> findBySubstitute(Person person) {
		return substituteAssignmentDao.findBySubstitute(person);
	}

	@Transactional
	public void Cleanup(int offsetDays) {
		log.info("Cleaning up substitute assignments");
		var managerUuids = orgUnitService.getAllActive().stream().filter(o -> o.getManager() != null).map(o -> o.getManager().getManager().getUuid()).toList();

		var assignments = findAll();

		for( var assignment : assignments ) {
			// only keep if manager is still a manager
			var keep = managerUuids.stream().anyMatch(uuid -> uuid.equalsIgnoreCase(assignment.getPerson().getUuid()));

			if( keep ) {
				// check if substitute still has valid affiliations
				var substituteValid = false;
				for( var affiliation : assignment.getSubstitute().getAffiliations() ) {
					if( affiliation.getStopDate() == null ) {
						substituteValid = true; // future affiliations are valid
					}
					else if( LocalDate.ofInstant(affiliation.getStopDate().toInstant(), ZoneId.systemDefault()).isAfter(LocalDate.now().minusDays(offsetDays))) {
						substituteValid = true; // recently expired affiliations are also valid
					}
				}
				keep = substituteValid;
			}

			if (keep) {
				// check if manager still has valid affiliations
				var managerValid = false;
				for( var affiliation : assignment.getPerson().getAffiliations() ) {
					if( affiliation.getStopDate() == null ) {
						managerValid = true; // future affiliations are valid
					}
					else if( LocalDate.ofInstant(affiliation.getStopDate().toInstant(), ZoneId.systemDefault()).isAfter(LocalDate.now().minusDays(offsetDays))) {
						managerValid = true; // recently expired affiliations are also valid
					}
				}
				keep = managerValid;
			}

			if( !keep ) {
				var message = "Stedfortræder (" + assignment.getContext().getName() + ") '" + assignment.getSubstitute().getEntityName() + "' for leder '" + assignment.getPerson().getEntityName() + "' slettet af oprydningsjob";
				log.info(message);
				auditLogger.log(String.valueOf(assignment.getId()), EntityType.SUBSTITUTE_ASSIGNMENT, EventType.DELETE,assignment.getSubstitute().getEntityName(), message);
				delete(assignment);
			}
		}
		log.info("Finished cleaning up substitute assignments");
	}
}
