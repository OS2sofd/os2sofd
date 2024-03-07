package dk.digitalidentity.sofd.dao;

import java.util.Date;
import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.EmailQueue;

public interface EmailQueueDao extends CrudRepository<EmailQueue, Long> {
	List<EmailQueue> findTop10ByDeliveryTtsBefore(Date tts);
	List<EmailQueue> findAllByCprAndDeliveryTtsAfter(String cpr, Date tts);
	List<EmailQueue> findAll();
	EmailQueue findById(long id);
}
