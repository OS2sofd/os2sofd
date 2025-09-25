package dk.digitalidentity.sofd.listener;

import dk.digitalidentity.sofd.dao.ReservedUsernameDao;
import dk.digitalidentity.sofd.dao.model.EntityChangeQueueDetail;
import dk.digitalidentity.sofd.listener.EntityListenerService.ChangeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RemovedADUserListener implements ListenerAdapter {

	@Autowired
	ReservedUsernameDao reservedUsernameDao;

	@Override
	public void personUpdated(String uuid, List<EntityChangeQueueDetail> changes) {
		// if a person's AD user was removed/deleted we delete reserved usernames for this person
		// this is because we want to force the username generator to generate a new username instead of using the old reserved one.
		if( changes.stream().anyMatch(c -> c.getChangeType() == ChangeType.REMOVED_AD_USER)) {
			reservedUsernameDao.deleteByPersonUuid(uuid);
		}
	}
}
