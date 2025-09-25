package dk.digitalidentity.sofd.dao;

import java.time.LocalDateTime;
import java.util.Date;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.SecurityLog;

public interface SecurityLogDao extends CrudRepository<SecurityLog, Long> {

	@Modifying
	@Query(nativeQuery = true, value = "DELETE FROM security_log WHERE timestamp < ?1 LIMIT 25000")
	void deleteByTimestampBefore(Date before);
	
	@Query(nativeQuery = true, value = "SELECT timestamp FROM security_log sl WHERE timestamp > ?2 AND sl.client_id = ?1 ORDER BY timestamp DESC LIMIT 1")
	Date getLastTimestampByClientId(long id, LocalDateTime dateTime);
}