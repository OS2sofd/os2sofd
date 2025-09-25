package dk.digitalidentity.sofd.dao.model.enums;

public enum AffiliationType {
	EMPLOYEE("html.enum.AffiliationType.employee"),
	EXTERNAL("html.enum.AffiliationType.external"),
	SUBSTITUTE("html.enum.AffiliationType.substitute");

	private String message;

	private AffiliationType(String message) { this.message = message; }

	public String getMessage() {
		return message;
	}
}
