package dk.digitalidentity.sofd.dao.model.enums;

import lombok.Getter;

@Getter
public enum ProfessionMatchType {
	POSITIVE("html.enum.professionMatchType.positive"),
	NEGATIVE("html.enum.professionMatchType.negative"),
	REGEX("html.enum.professionMatchType.regex");

	private String message;

	private ProfessionMatchType(String message) {
		this.message = message;
	}
}
