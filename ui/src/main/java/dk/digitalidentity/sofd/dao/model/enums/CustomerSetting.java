package dk.digitalidentity.sofd.dao.model.enums;

import lombok.Getter;

@Getter
public enum CustomerSetting {
	LAST_STSSYNC_RUN("0"),
	LAST_TELEPHONY_REVISION("0"),
	OPUS_AUTO_AFF(""),
	USER_INACTIVE_PERIOD("3"),
	ACCOUNT_APPROVAL_DEPLOYED(""),
	COMPLETED_FICTIVE_CPR_MIGRATION("NO");

	private String defaultValue;

	private CustomerSetting(String defaultValue) {
		this.defaultValue = defaultValue;
	}
}
