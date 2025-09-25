package dk.digitalidentity.sofd.dao.model;

import java.util.Date;

public interface RevisionId {
	Integer getRev();

	Date getLastChanged();

	String getAuditorName();
}
