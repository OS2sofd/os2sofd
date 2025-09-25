package dk.digitalidentity.sofd.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.SettingDao;
import dk.digitalidentity.sofd.dao.model.Setting;

@Service
public class PwdLockedService {
	private static final String SETTING_PWD_LOCKED_SMS_TEXT = "PwdLockedSmsTxt";
	private static final String SETTING_PWD_LOCKED_ENABLED = "PwdLockedEnabled";

	@Autowired
	private SettingDao settingDao;

	public boolean getPwdLockedEnabled() {
		Setting setting = settingDao.findByKey(SETTING_PWD_LOCKED_ENABLED);
		if (setting == null) {
			return false;
		}

		return Boolean.parseBoolean(setting.getValue());
	}

	public void setPwdLockedEnabled(boolean enabled) {
		Setting setting = settingDao.findByKey(SETTING_PWD_LOCKED_ENABLED);
		if (setting == null) {
			setting = new Setting();
			setting.setKey(SETTING_PWD_LOCKED_ENABLED);
		}

		setting.setValue(enabled ? "true" : "false");
		settingDao.save(setting);
	}

	public String getPwdReminderSmsTxt() {
		Setting setting = settingDao.findByKey(SETTING_PWD_LOCKED_SMS_TEXT);
		if (setting == null) {
			return "Din konto {KONTO} er blevet låst på grund af for mange fejlede login forsøg";
		}

		return setting.getValue();
	}

	public void setPwdReminderSmsTxt(String text) {
		Setting setting = settingDao.findByKey(SETTING_PWD_LOCKED_SMS_TEXT);
		if (setting == null) {
			setting = new Setting();
			setting.setKey(SETTING_PWD_LOCKED_SMS_TEXT);
		}

		setting.setValue(text);
		settingDao.save(setting);
	}
}
