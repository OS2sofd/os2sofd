package dk.digitalidentity.sofd.dao.model.enums;

import lombok.Getter;

@Getter
public enum UsernameInfixType {
	RANDOM("html.usertype.username.infix.random"),
	SAME_AS_OTHER("html.usertype.username.infix.same_as_other"),
	FROM_NAME("html.usertype.username.infix.from_name"),
	FROM_NAME_LONG("html.usertype.username.infix.from_name_long"),
	EMPLOYEE_ID("html.usertype.username.infix.employee_id");

	private String message;

	private UsernameInfixType(String message) {
		this.message = message;
	}
}
