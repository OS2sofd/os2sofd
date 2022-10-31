package dk.digitalidentity.sofd.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.model.ModificationHistory;
import dk.digitalidentity.sofd.dao.model.enums.CustomerSetting;
import dk.digitalidentity.sofd.service.model.AuditWrapper;
import dk.digitalidentity.sofd.service.model.LatestModificationsWrapper;

@Service
public class ModificationHistoryService {
	// TODO: if the SELECT MAX(id) performs bad, a trick is to use "SELECT id FROM revisions ORDER BY id DESC LIMIT 1"
	private static final String maxQuery = "SELECT MAX(id) FROM revisions";
	private static final String insertQuery = "INSERT INTO modification_history (`changed`,`uuid`,`entity`,`change_type`) VALUES (?, ?, ?, ?);";
	private static final String deleteQuery = "DELETE FROM modification_history WHERE changed < NOW() - INTERVAL ? DAY";

	@Qualifier("defaultTemplate")
	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private SettingService settingService;

	/**
	 * Returns LatestModificationsWrapper object that contains:</br>
	 * <ul>
	 * <li>latestRevisionNumber</li>
	 * <li>list of Person changes</li>
	 * <li>list of OrgUnit changes</li>
	 * </ul>
	 * if there are no changes returns null
	 */
	@SuppressWarnings("deprecation")
	public LatestModificationsWrapper getLatestModifications() {
		LatestModificationsWrapper result = new LatestModificationsWrapper();
		Long latestRevisionNumber = jdbcTemplate.queryForObject(maxQuery, Long.class);
		Long currentRevisionNumber = settingService.getLongValueByKey(CustomerSetting.LAST_READ_REVISION);
		
		if (latestRevisionNumber == null) {
			//no changes in the database
			return null;
		}
		
		if (currentRevisionNumber == null || currentRevisionNumber < latestRevisionNumber) {
			result.setLatestRevisionNumber(latestRevisionNumber);

			if (currentRevisionNumber == null) {
				currentRevisionNumber = 0l; // in sql query we cannot compare number to null.
			}

			// get all persons
			result.setPersonChanges(jdbcTemplate.query(getSelectSQL("persons_aud"), new Object[] { currentRevisionNumber, latestRevisionNumber }, (RowMapper<AuditWrapper>) (rs, rowNum) -> {
				return new AuditWrapper(rs);
			}));

			// get all orgunits
			result.setOrgUnitChanges(jdbcTemplate.query(getSelectSQL("orgunits_aud"), new Object[] { currentRevisionNumber, latestRevisionNumber }, (RowMapper<AuditWrapper>) (rs, rowNum) -> {
				return new AuditWrapper(rs);
			}));

			return result;
		}

		return null;
	}

	public void insert(ModificationHistory modificationHistory) {
		jdbcTemplate.update(insertQuery, modificationHistory.getChanged(), modificationHistory.getUuid(), modificationHistory.getEntity().toString(), modificationHistory.getChangeType().toString());
	}

	public int removeModificationHistoryOlderThan(int days) {
		return jdbcTemplate.update(deleteQuery, days);
	}
	
	private String getSelectSQL(String tableName) {
		return "SELECT uuid, revtype FROM " + tableName + " WHERE " + tableName + ".rev > ? AND " + tableName + ".rev <= ?;";
	}
}
