package dk.digitalidentity.sofd.listener;

import java.util.List;

import org.springframework.stereotype.Component;

import dk.digitalidentity.sofd.dao.model.EntityChangeQueueDetail;

@Component
public interface ListenerAdapter {
	default void orgUnitCreated(String uuid) { };
	default void orgUnitDeleted(String uuid) { };
	default void orgUnitUpdated(String uuid, List<EntityChangeQueueDetail> changes) { };

	default void personCreated(String uuid) { };
	default void personDeleted(String uuid) { };
	default void personUpdated(String uuid, List<EntityChangeQueueDetail> changes) { };
}
