package dk.digitalidentity.sofd.service.model;

import lombok.Getter;

@Getter
public enum RequestEmailType {
	SAME_AS_AD("html.setting.requestemailtype.same_as_ad"),
	EMPLOYEE_NAME("html.setting.requestemailtype.employee_name"),
	EMPLOYEE_NAME_LONG("html.setting.requestemailtype.employee_name_long");
	
	private String message;
	
	private RequestEmailType(String message) {
		this.message = message;
	}
}
