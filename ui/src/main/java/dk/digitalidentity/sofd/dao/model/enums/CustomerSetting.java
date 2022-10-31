package dk.digitalidentity.sofd.dao.model.enums;

import lombok.Getter;

@Getter
public enum CustomerSetting {
	LAST_READ_REVISION("0"), LAST_STSSYNC_RUN("0"), LAST_TELEPHONY_REVISION("0"), OPUS_AUTO_AFF(""), USER_INACTIVE_PERIOD("3");

	private String defaultValue;

	private CustomerSetting(String defaultValue) {
		this.defaultValue = defaultValue;
	}
}
