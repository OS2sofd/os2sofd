package dk.digitalidentity.sofd.dao;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.SmsLog;

public interface SmsLogDao extends CrudRepository<SmsLog, Long> {
	List<SmsLog> findAll();

	Optional<SmsLog> getById(long id);
}