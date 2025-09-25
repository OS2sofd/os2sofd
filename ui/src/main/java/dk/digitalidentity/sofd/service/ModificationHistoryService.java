package dk.digitalidentity.sofd.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.model.ModificationHistory;

@Service
public class ModificationHistoryService {
	private static final String insertQuery = "INSERT INTO modification_history (`changed`,`uuid`,`entity`,`change_type`) VALUES (?, ?, ?, ?);";
	private static final String deleteQuery = "DELETE FROM modification_history WHERE changed < NOW() - INTERVAL ? DAY";

	@Qualifier("defaultTemplate")
	@Autowired
	private JdbcTemplate jdbcTemplate;

	public void insert(ModificationHistory modificationHistory) {
		jdbcTemplate.update(insertQuery, modificationHistory.getChanged(), modificationHistory.getUuid(), modificationHistory.getEntity().toString(), modificationHistory.getChangeType().toString());
	}

	public int removeModificationHistoryOlderThan(int days) {
		return jdbcTemplate.update(deleteQuery, days);
	}
}
