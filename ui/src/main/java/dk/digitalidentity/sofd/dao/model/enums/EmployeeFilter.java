package dk.digitalidentity.sofd.dao.model.enums;

import lombok.Getter;

@Getter
public enum EmployeeFilter {
	ALL("html.entity.email_template.employee_filter.all"),
	ONLY_EMPLOYEES("html.entity.email_template.employee_filter.only_employees"),
	ONLY_EXTERNAL("html.entity.email_template.employee_filter.only_external");

	private String message;

	EmployeeFilter(String message) {
		this.message = message;
	}

}
