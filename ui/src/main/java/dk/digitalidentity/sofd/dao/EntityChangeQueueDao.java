package dk.digitalidentity.sofd.dao;

import java.util.Date;
import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.EntityChangeQueue;

public interface EntityChangeQueueDao extends CrudRepository<EntityChangeQueue, Long> {

	List<EntityChangeQueue> findByTtsBefore(Date tts);

}
