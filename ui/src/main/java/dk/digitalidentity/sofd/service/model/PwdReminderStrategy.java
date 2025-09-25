package dk.digitalidentity.sofd.service.model;

import lombok.Getter;

@Getter
public enum PwdReminderStrategy {
	DISABLED("html.setting.pwdreminderstrategy.disabled"),
	SMS_ONLY("html.setting.pwdreminderstrategy.sms"),
	EMAIL_ONLY("html.setting.pwdreminderstrategy.email"),
	SMS_AND_EMAIL("html.setting.pwdreminderstrategy.both"),
	SMS_FIRST_OTHERWISE_EMAIL("html.setting.pwdreminderstrategy.sms_otherwise_email"),
	EMAIL_FIRST_OTHERWISE_SMS("html.setting.pwdreminderstrategy.email_otherwise_sms");

	private String message;
	
	private PwdReminderStrategy(String message) {
		this.message = message;
	}
}
