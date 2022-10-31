package dk.digitalidentity.sofd.dao.model.enums;

public enum Visibility {
	VISIBLE("html.enum.visibility.visible"),
	HIDDEN_FOR_CITIZENS("html.enum.visibility.hiddenForCitizens"),
	HIDDEN_FOR_EVERYBODY("html.enum.visibility.hiddenForEverybody");

	private String message;

	Visibility(String message) { this.message = message; }

	public String getMessage() {
		return message;
	}
}
