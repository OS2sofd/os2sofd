package dk.digitalidentity.sofd.service.model;

import lombok.Getter;

@Getter
public enum UsernameSuffix {
	NONE("html.setting.username.suffix.none"),
	CREATE_DATE("html.setting.username.suffix.create_date"),
	EMPLOYEE_ID("html.setting.username.suffix.employee_id");

	private String message;

	private UsernameSuffix(String message) {
		this.message = message;
	}
}
