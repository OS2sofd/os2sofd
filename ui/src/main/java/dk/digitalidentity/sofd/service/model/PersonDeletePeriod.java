package dk.digitalidentity.sofd.service.model;

import lombok.Getter;

@Getter
public enum PersonDeletePeriod {
	NEVER("html.setting.persondeleteperiod.never"),
	MONTH_6("html.setting.persondeleteperiod.month6"),
	MONTH_12("html.setting.persondeleteperiod.month12"),
	MONTH_36("html.setting.persondeleteperiod.month36"),
	MONTH_60("html.setting.persondeleteperiod.month60");

	private String message;

	private PersonDeletePeriod(String message) {
		this.message = message;
	}
}
