package dk.digitalidentity.sofd.service.model;

import java.sql.ResultSet;
import java.sql.SQLException;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuditWrapper {
	private String uuid;
	private ChangeType changeType;

	public AuditWrapper(String uuid, ChangeType changeType) {
		this.uuid = uuid;
		this.changeType = changeType;
	}

	public AuditWrapper(ResultSet rs) throws SQLException {
		uuid = rs.getString("uuid");
		String revType = rs.getString("revtype");

		switch (revType) {
			case "0":
				changeType = ChangeType.CREATE;
				break;
			case "1":
				changeType = ChangeType.UPDATE;
				break;
			case "2":
				changeType = ChangeType.DELETE;
				break;
			default:
				throw new SQLException("Error occured while parsing revType.");
		}
	}
}
