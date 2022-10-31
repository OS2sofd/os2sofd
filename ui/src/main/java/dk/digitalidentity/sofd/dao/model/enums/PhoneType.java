package dk.digitalidentity.sofd.dao.model.enums;

public enum PhoneType {
	MOBILE("html.enum.phoneType.mobile"),
	BROADBAND("html.enum.phoneType.broadband"),
	IP("html.enum.phoneType.ip"),
	DATA_SIM("html.enum.phoneType.data"),
	LANDLINE("html.enum.phoneType.landline"),
	DEPARTMENT_NUMBER("html.enum.phoneType.department_number"),
	FAX_NUMBER("html.enum.phoneType.fax_number");

	private String message;

	private PhoneType(String message) { this.message = message; }

	public String getMessage() {
		return message;
	}
}
