package dk.digitalidentity.sofd.service;

import dk.digitalidentity.sofd.dao.WorkplaceDao;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.ModificationHistory;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.Workplace;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.service.model.ChangeType;
import dk.digitalidentity.sofd.service.model.WorkplacePeriod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WorkplaceService {
	
	@Autowired
	private WorkplaceDao workplaceDao;

	@Autowired
	private ModificationHistoryService modificationHistoryService;

	@Autowired
	private OrgUnitService orgUnitService;

	@Transactional
	public void findWorkplaceModifications() {
		LocalDate today = LocalDate.now();
		List<Workplace> startedWorkplaces = workplaceDao.findByStartDate(today);
		List<Workplace> stoppedWorkplaces = workplaceDao.findByStopDate(today);
		List<Person> changedPeople = startedWorkplaces.stream().map(w -> w.getAffiliation().getPerson()).collect(Collectors.toList());
		changedPeople.addAll(stoppedWorkplaces.stream().map(w -> w.getAffiliation().getPerson()).collect(Collectors.toList()));

		for (Person changedPerson : changedPeople) {
			ModificationHistory modificationHistory = new ModificationHistory();
			modificationHistory.setUuid(changedPerson.getUuid());
			modificationHistory.setChanged(new Date());
			modificationHistory.setEntity(EntityType.PERSON);
			modificationHistory.setChangeType(ChangeType.UPDATE);
			modificationHistoryService.insert(modificationHistory);
		}
	}

	public List<Workplace> findAll() {
		return workplaceDao.findAll();
	}

	public boolean setWorkplaces(Affiliation matchingAffiliation, List<WorkplacePeriod> workplacePeriods) {
		var newWorkplaces = new ArrayList<Workplace>();
		workplacePeriods.sort(Comparator.comparing(WorkplacePeriod::getStartDate));
		LocalDate previousStopDate = null;
		for( var workplacePeriod : workplacePeriods ) {
			if( previousStopDate != null  && !workplacePeriod.getStartDate().isAfter(previousStopDate) ) {
				log.warn("Invalid workplaceperiod for affiliation with masterId " + matchingAffiliation.getMasterId() + " - start date not after previous stop date");
				return false;
			}
			if( workplacePeriod.getStopDate().isBefore(workplacePeriod.getStartDate()) ) {
				log.warn("Invalid workplaceperiod for affiliation with masterId " + matchingAffiliation.getMasterId() + " - stop date before start date");
				return false;
			}
			var orgUnit = orgUnitService.getByUuid(workplacePeriod.getOrgUnitUuid());
			if( orgUnit == null ) {
				log.warn("Invalid workplaceperiod for affiliation with masterId " + matchingAffiliation.getMasterId() + " - orgunit not found. Uuid: " + workplacePeriod.getOrgUnitUuid());
				return false;
			}
			var newWorkplace = new Workplace();
			newWorkplace.setAffiliation(matchingAffiliation);
			newWorkplace.setOrgUnit(orgUnit);
			newWorkplace.setStartDate(workplacePeriod.getStartDate());
			newWorkplace.setStopDate(workplacePeriod.getStopDate());
			newWorkplaces.add(newWorkplace);
			previousStopDate = workplacePeriod.getStopDate();
		}
		matchingAffiliation.getWorkplaces().removeIf(w -> true);
		matchingAffiliation.getWorkplaces().addAll(newWorkplaces);
		return true;
	}
}
