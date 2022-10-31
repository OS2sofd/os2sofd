package dk.digitalidentity.sofd.dao;

import java.util.Date;
import java.util.List;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.FutureEmail;

public interface FutureEmailsDao extends CrudRepository<FutureEmail, Long> {
	List<FutureEmail> findAll();

	List<FutureEmail> findByDeliveryTtsBefore(Date tts);
}
