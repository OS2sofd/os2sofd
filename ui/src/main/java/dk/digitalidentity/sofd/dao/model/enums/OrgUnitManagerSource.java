package dk.digitalidentity.sofd.dao.model.enums;

public enum OrgUnitManagerSource {
	SELECTED("html.enum.orgUnitManagerSource.selected"),
	IMPORTED("html.enum.orgUnitManagerSource.imported");
	private String message;

	private OrgUnitManagerSource(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}
}
