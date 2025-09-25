package dk.digitalidentity.sofd.dao.model.enums;

public enum PersonType {
	PERSON("html.enum.personType.person"),
	ROBOT("html.enum.personType.robot");

	private String message;

	private PersonType(String message) {
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

}
