package dk.digitalidentity.sofd.listener;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.dao.model.EntityChangeQueueDetail;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.OrgUnitChange;
import dk.digitalidentity.sofd.dao.model.enums.OrgUnitChangeChangeType;
import dk.digitalidentity.sofd.dao.model.enums.OrgUnitChangeStatus;
import dk.digitalidentity.sofd.listener.EntityListenerService.ChangeType;
import dk.digitalidentity.sofd.service.OrgUnitChangeService;
import dk.digitalidentity.sofd.service.OrgUnitService;

@Component
public class OrgUnitChangesListener implements ListenerAdapter {

	@Autowired
	private OrgUnitService orgUnitService;
	
	@Autowired
	private OrgUnitChangeService orgUnitChangeService;
	
	@Override
	public void orgUnitCreated(String uuid) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			return;
		}
		
		OrgUnitChange orgUnitChange = new OrgUnitChange();
		orgUnitChange.setChangeType(OrgUnitChangeChangeType.NEW);
		orgUnitChange.setNewValue(orgUnit.getName());
		orgUnitChange.setOrgUnit(orgUnit);
		orgUnitChange.setChangedTimestamp(new Date());
		orgUnitChange.setStatus(OrgUnitChangeStatus.PENDING);
		
		orgUnitChangeService.save(orgUnitChange);
		
	}

	@Override
	public void orgUnitUpdated(String uuid, List<EntityChangeQueueDetail> changes) {
		List<EntityChangeQueueDetail> orgUnitEntityChanges = changes.stream().filter(c -> c.getChangeType().equals(ChangeType.CHANGED_NAME) || c.getChangeType().equals(ChangeType.CHANGED_PARENT_UUID)).collect(Collectors.toList());
		if (orgUnitEntityChanges.isEmpty()) {
			return;
		}

		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			return;
		}
		
		for (EntityChangeQueueDetail orgUnitEntityChange : orgUnitEntityChanges) {
			OrgUnitChangeChangeType changeType = OrgUnitChangeChangeType.NAME_CHANGE;
			
			if (orgUnitEntityChange.getChangeType().equals(ChangeType.CHANGED_PARENT_UUID)) {
				changeType = OrgUnitChangeChangeType.NEW_PARENT;
			}
			
			OrgUnitChange orgUnitChange = new OrgUnitChange();
			orgUnitChange.setChangeType(changeType);
			orgUnitChange.setOldValue(orgUnitEntityChange.getOldValue());
			orgUnitChange.setNewValue(orgUnitEntityChange.getNewValue());
			orgUnitChange.setOrgUnit(orgUnit);
			orgUnitChange.setChangedTimestamp(new Date());
			orgUnitChange.setStatus(OrgUnitChangeStatus.PENDING);
			
			orgUnitChangeService.save(orgUnitChange);
		}
		
	}
}
