package dk.digitalidentity.sofd.dao.model;

public interface RevisionId {
	Integer getRev();

	// epoch milliseconds (UTC), converted to local time by the browser
	Long getLastChanged();

	String getAuditorName();
}
