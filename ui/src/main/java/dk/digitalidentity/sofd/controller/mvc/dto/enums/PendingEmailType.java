package dk.digitalidentity.sofd.controller.mvc.dto.enums;

import lombok.Getter;

@Getter
public enum PendingEmailType {
	DIGITALPOST("html.enum.email.type.eboks"),
	EMAIL("html.enum.email.type.email");

	private String message;

	PendingEmailType(String message) {
		this.message = message;
	}
}
