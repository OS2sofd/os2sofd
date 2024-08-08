package dk.digitalidentity.sofd.dao.model.enums;

import lombok.Getter;

@Getter
public enum CustomerSetting {
	LAST_STSSYNC_RUN("0"),
	LAST_TELEPHONY_REVISION("0"),
	OPUS_AUTO_AFF(""),
	USER_INACTIVE_PERIOD("3"),
	COMPLETED_KOMBIT_UUID_MIGRATION("NO"),
	ACCOUNT_APPROVAL_DEPLOYED("");

	private String defaultValue;

	private CustomerSetting(String defaultValue) {
		this.defaultValue = defaultValue;
	}
}
