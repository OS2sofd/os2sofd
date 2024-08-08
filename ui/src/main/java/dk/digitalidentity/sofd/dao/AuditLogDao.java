package dk.digitalidentity.sofd.dao;

import java.util.Date;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import dk.digitalidentity.sofd.dao.model.AuditLog;

public interface AuditLogDao extends CrudRepository<AuditLog, Long> {
	AuditLog findById(long id);

	@Modifying
	@Query(nativeQuery = true, value = "DELETE FROM audit_log WHERE timestamp < ?1 LIMIT 25000")
	void deleteByTimestampBefore(Date before);
}