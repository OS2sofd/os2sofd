package dk.digitalidentity.sofd.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dk.digitalidentity.sofd.dao.SubstituteOrgUnitAssignmentDao;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.SubstituteContext;
import dk.digitalidentity.sofd.dao.model.SubstituteOrgUnitAssignment;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.dao.model.enums.EventType;
import dk.digitalidentity.sofd.log.AuditLogger;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SubstituteOrgUnitAssignmentService {

	@Autowired
	private SubstituteOrgUnitAssignmentDao substituteOrgUnitAssignmentDao;

	@Autowired
	private AuditLogger auditLogger;

	public SubstituteOrgUnitAssignment getById(long id) {
		return substituteOrgUnitAssignmentDao.getById(id);
	}

	public void delete(SubstituteOrgUnitAssignment assignment) {
		substituteOrgUnitAssignmentDao.delete(assignment);
	}

	public SubstituteOrgUnitAssignment save(SubstituteOrgUnitAssignment assignment) {
		assignment.setChanged(LocalDateTime.now());
		return substituteOrgUnitAssignmentDao.save(assignment);
	}
	public List<SubstituteOrgUnitAssignment> findBySubstitute(Person person) {
		return substituteOrgUnitAssignmentDao.findBySubstitute(person);
	}
	public List<SubstituteOrgUnitAssignment> findByContext(SubstituteContext context) {
		return substituteOrgUnitAssignmentDao.findByContext(context);
	}

	public List<SubstituteOrgUnitAssignment> getAll() {
		return substituteOrgUnitAssignmentDao.findAll();
	}
	public void deleteAllByContext(SubstituteContext context) {
		substituteOrgUnitAssignmentDao.deleteAll(findByContext(context));
	}

    @Transactional
	public void Cleanup(int offsetDays) {
		log.info("Cleaning up substitute orgunit assignments");
		var assignments = getAll();

		for( var assignment : assignments ) {
			// check if substitute still has valid affiliations
			var substituteValid = false;
			for (var affiliation : assignment.getSubstitute().getAffiliations()) {
				if (affiliation.getStopDate() == null) {
					substituteValid = true; // future affiliations are valid
				} else if (LocalDate.ofInstant(affiliation.getStopDate().toInstant(), ZoneId.systemDefault()).isAfter(LocalDate.now().minusDays(offsetDays))) {
					substituteValid = true; // recently expired affiliations are also valid
				}
			}
			if( !substituteValid ) {
				var message = "Stedfortræder (" + assignment.getContext().getName() + ") '" + assignment.getSubstitute().getEntityName() + "' for enhed '" + assignment.getOrgUnit().getEntityName() + "' slettet af oprydningsjob";
				log.info(message);
				auditLogger.log(String.valueOf(assignment.getId()), EntityType.SUBSTITUTE_ORGUNIT_ASSIGNMENT, EventType.DELETE,assignment.getSubstitute().getEntityName(), message);
				delete(assignment);

			}
		}
		log.info("Finished cleaning up substitute orgunit assignments");
    }

	public List<String> getSofdSubstituteUuids(String orgUnitUuid) {
		return substituteOrgUnitAssignmentDao.getSofdSubstituteUuids(orgUnitUuid);
	}
}
