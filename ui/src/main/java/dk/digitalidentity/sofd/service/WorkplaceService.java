package dk.digitalidentity.sofd.service;

import dk.digitalidentity.sofd.dao.WorkplaceDao;
import dk.digitalidentity.sofd.dao.model.ModificationHistory;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.Workplace;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;
import dk.digitalidentity.sofd.service.model.ChangeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WorkplaceService {
	
	@Autowired
	private WorkplaceDao workplaceDao;

	@Autowired
	private ModificationHistoryService modificationHistoryService;

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
}
