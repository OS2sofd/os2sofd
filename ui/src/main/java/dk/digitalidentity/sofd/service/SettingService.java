package dk.digitalidentity.sofd.service;

import java.util.Objects;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.SettingDao;
import dk.digitalidentity.sofd.dao.model.Setting;
import dk.digitalidentity.sofd.dao.model.enums.CustomerSetting;
import dk.digitalidentity.sofd.dao.model.enums.NotificationType;
import dk.digitalidentity.sofd.service.model.PersonDeletePeriod;

@Service
public class SettingService {
	private static final String SETTING_PERSON_DELETE_PERIOD = "PersonDeletePeriod";
	private static final String SETTING_SCHEDULED_TASKS_RUNNING = "ScheduledTasksRunning";

	@Autowired
	private SettingDao settingDao;

	@Transactional
	public void setScheduledTasksRunning() {
		Setting setting = settingDao.findByKey(SETTING_SCHEDULED_TASKS_RUNNING);
		if (setting == null) {
			setting = new Setting();
			setting.setKey(SETTING_SCHEDULED_TASKS_RUNNING);
		}
		
		setting.setValue("true");
		settingDao.save(setting);
	}

	public boolean isScheduledTasksRunning() {
		Setting setting = settingDao.findByKey(SETTING_SCHEDULED_TASKS_RUNNING);
		if (setting != null && Objects.equals("true", setting.getValue())) {
			return true;
		}
		
		return false;
	}

	public void clearScheduledTasksRunning() {
		Setting setting = settingDao.findByKey(SETTING_SCHEDULED_TASKS_RUNNING);
		if (setting == null) {
			setting = new Setting();
			setting.setKey(SETTING_SCHEDULED_TASKS_RUNNING);
		}
		
		setting.setValue("false");
		settingDao.save(setting);
	}
	
	public PersonDeletePeriod getPersonDeletePeriod() {
		String value = getKeyWithDefault(SETTING_PERSON_DELETE_PERIOD, PersonDeletePeriod.NEVER.toString());
		
		return PersonDeletePeriod.valueOf(value);
	}

	public void setPersonDeletePeriod(PersonDeletePeriod period) {
		Setting setting = settingDao.findByKey(SETTING_PERSON_DELETE_PERIOD);
		if (setting == null) {
			setting = new Setting();
			setting.setKey(SETTING_PERSON_DELETE_PERIOD);
		}
		
		setting.setValue(period.toString());
		settingDao.save(setting);
	}

	public boolean isNotificationTypeEnabled(NotificationType notificationType) {
		return getBooleanWithDefault(notificationType.toString(), notificationType.isDefaultEnabled());
	}

	public void setNotificationTypeEnabled(NotificationType notificationType, boolean enabled) {
		Setting setting = settingDao.findByKey(notificationType.toString());
		if (setting == null) {
			setting = new Setting();
			setting.setKey(notificationType.toString());
		}

		setting.setValue(Boolean.toString(enabled));
		settingDao.save(setting);
	}

	public long getLastUserNameNumberUsed(String userType)
	{
		var key = "LAST_USERNAME_NUMBER_USED:" + userType;
		Setting setting = settingDao.findByKey(key);
		return setting != null ? Long.parseLong(setting.getValue()) : 0;
	}

	public void setLastUserNameNumberUsed(String userType, long number)
	{
		var key = "LAST_USERNAME_NUMBER_USED:" + userType;
		Setting setting = settingDao.findByKey(key);
		if (setting == null) {
			setting = new Setting();
			setting.setKey(key);
		}

		setting.setValue(String.valueOf(number));
		settingDao.save(setting);
	}

	/// helper methods

	private String getKeyWithDefault(String key, String defaultValue) {
		Setting setting = settingDao.findByKey(key);
		if (setting != null) {
			return setting.getValue();
		}

		return defaultValue;
	}

	private boolean getBooleanWithDefault(String key, boolean defaultValue) {
		Setting setting = settingDao.findByKey(key);
		if (setting != null) {
			return Boolean.parseBoolean(setting.getValue());
		}

		return defaultValue;
	}

	// Another way of managing settings

	public Long getLongValueByKey(CustomerSetting customerSetting) {
		Long value;

		Setting setting = getByKey(customerSetting);

		if (setting != null) {
			value = Long.parseLong(setting.getValue());
		}
		else {
			Setting newSetting = new Setting();
			newSetting.setKey(customerSetting.toString());
			newSetting.setValue(customerSetting.getDefaultValue());
			settingDao.save(newSetting);
			value = Long.parseLong(customerSetting.getDefaultValue());
		}

		return value;
	}

	public Setting getByKey(CustomerSetting key) {
		return settingDao.findByKey(key.toString());
	}
	
	public void setUserInactivePeriod(Long days) {
		Setting setting = getByKey(CustomerSetting.USER_INACTIVE_PERIOD);
		if (setting == null) {
			setting = new Setting();
			setting.setKey(CustomerSetting.USER_INACTIVE_PERIOD.toString());
		}
		
		setting.setValue(days.toString());
		settingDao.save(setting);
	}

	public void save(Setting setting) {
		settingDao.save(setting);
	}

	public Setting getOpusAutoAffiliations() {
		Setting setting = settingDao.findByKey(CustomerSetting.OPUS_AUTO_AFF.toString());
		if (setting == null) {
			setting = new Setting();
			setting.setKey(CustomerSetting.OPUS_AUTO_AFF.toString());
			setting.setValue(CustomerSetting.OPUS_AUTO_AFF.getDefaultValue());
		}
		
		return setting;
	}
	
	public void setValueForKey(String key, boolean enabled) {
		Setting setting = settingDao.findByKey(key);
		if (setting == null) {
			setting = new Setting();
			setting.setKey(key);
		}

		setting.setValue(Boolean.toString(enabled));
		settingDao.save(setting);
	}
}
