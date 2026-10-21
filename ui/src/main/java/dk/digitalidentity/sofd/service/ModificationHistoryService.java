package dk.digitalidentity.sofd.service;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.service.model.ChangeType;
import dk.digitalidentity.sofd.dao.model.enums.EntityType;

@Service
public class ModificationHistoryService {
	private static final String insertQuery = "INSERT INTO modification_history (`changed`,`uuid`,`entity`,`change_type`) VALUES (?, ?, ?, ?);";
	private static final String deleteQuery = "DELETE FROM modification_history WHERE changed < NOW() - INTERVAL ? DAY";

	@Qualifier("defaultTemplate")
	@Autowired
	private JdbcTemplate jdbcTemplate;

	public void insert(EntityType entity, String uuid, ChangeType changeType) {
		jdbcTemplate.update(insertQuery, new Date(), uuid, entity.toString(), changeType.toString());
	}

	public void insertAll(EntityType entity, Collection<String> uuids, ChangeType changeType) {
		if (uuids == null || uuids.isEmpty()) {
			return;
		}

		Date now = new Date();
		List<Object[]> rows = uuids.stream()
				.map(uuid -> new Object[] { now, uuid, entity.toString(), changeType.toString() })
				.toList();

		jdbcTemplate.batchUpdate(insertQuery, rows);
	}

	public int removeModificationHistoryOlderThan(int days) {
		return jdbcTemplate.update(deleteQuery, days);
	}
}
