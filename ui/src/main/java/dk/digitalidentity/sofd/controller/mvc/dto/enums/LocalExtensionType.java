package dk.digitalidentity.sofd.controller.mvc.dto.enums;

import lombok.Getter;

@Getter
public enum LocalExtensionType {
	PERSON("html.enum.localExtensionType.Person"),
	USER("html.enum.localExtensionType.User"),
	AFFILIATION("html.enum.localExtensionType.Employment");

	private String message;

	private LocalExtensionType(String message) {
		this.message = message;
	}
}