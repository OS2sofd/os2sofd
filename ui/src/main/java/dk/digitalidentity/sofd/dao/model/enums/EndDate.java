package dk.digitalidentity.sofd.dao.model.enums;

public enum EndDate {
	NO("html.enum.endDate.no"),
	TOMORROW("html.enum.endDate.tomorrow"),
	DAYS3("html.enum.endDate.3.days"),
	WEEK("html.enum.endDate.week"),
	MONTH("html.enum.endDate.month");

	private String message;

	private EndDate(String message) { this.message = message; }

	public String getMessage() {
		return message;
	}
}
