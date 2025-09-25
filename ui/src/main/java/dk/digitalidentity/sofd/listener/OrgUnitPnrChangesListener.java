package dk.digitalidentity.sofd.listener;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import dk.digitalidentity.sofd.controller.mvc.dto.PUnitDTO;
import dk.digitalidentity.sofd.dao.model.EntityChangeQueueDetail;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.listener.EntityListenerService.ChangeType;
import dk.digitalidentity.sofd.service.CvrService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class OrgUnitPnrChangesListener implements ListenerAdapter {

	@Autowired
	private OrgUnitService orgUnitService;
	
	@Autowired
	private CvrService cvrService;
	
	@Override
	public void orgUnitCreated(String uuid) {
		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			return;
		}
		
		if (orgUnit.getPnr() != null) {
			PUnitDTO pUnitDTO = cvrService.getPUnitByPnr(orgUnit.getPnr().toString());
			if (pUnitDTO != null) {
				orgUnit.setCvrName(pUnitDTO.getName());
				try {
					orgUnitService.save(orgUnit);
				} catch (Exception e) {
					log.warn("Failed to save orgUnit after cvr name change name. Orgunit with uuid: " + orgUnit.getUuid(),e);
				}
			}
		}
		
		
	}

	@Override
	public void orgUnitUpdated(String uuid, List<EntityChangeQueueDetail> changes) {
		EntityChangeQueueDetail orgUnitPnrChange = changes.stream().filter(c -> c.getChangeType().equals(ChangeType.CHANGED_PNR)).findAny().orElse(null);
		if (orgUnitPnrChange == null) {
			return;
		}

		OrgUnit orgUnit = orgUnitService.getByUuid(uuid);
		if (orgUnit == null) {
			return;
		}

		if (orgUnit.getPnr() == null) {
			return;
		}

		PUnitDTO pUnitDTO = cvrService.getPUnitByPnr(orgUnit.getPnr().toString());
		if (pUnitDTO != null) {
			orgUnit.setCvrName(pUnitDTO.getName());
			try {
				orgUnitService.save(orgUnit);
			} catch (Exception e) {
				log.warn("Failed to save orgUnit after cvr name change name. Orgunit with uuid: " + orgUnit.getUuid(),e);
			}
		}
		
	}
}
