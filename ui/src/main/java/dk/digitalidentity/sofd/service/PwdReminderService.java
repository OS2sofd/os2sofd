package dk.digitalidentity.sofd.service;

import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import dk.digitalidentity.sofd.dao.SettingDao;
import dk.digitalidentity.sofd.dao.model.Setting;
import dk.digitalidentity.sofd.service.model.PwdReminderStrategy;

import static dk.digitalidentity.sofd.service.model.PwdReminderStrategy.*;

@Service
public class PwdReminderService {
	private static final String SETTING_PWD_REMINDER_SMS_TEXT = "PwdReminderSmsTxt";
	private static final String SETTING_PWD_REMINDER_EMAIL_TEXT = "PwdReminderEmailTxt";
	private static final String SETTING_PWD_REMINDER_EMAIL_SUBJECT = "PwdReminderEmailSubject";
	private static final String SETTING_PWD_REMINDER_STRATEGY = "PwdReminderStrategy";
	private static final String SETTING_PWD_REMINDER_TIME = "PwdReminderTime";
	private static final String SETTING_PWD_REMINDER_DAYS_BEFORE = "PwdReminderDaysBefore";
	private static final String SETTING_PWD_REMINDER_ORGUNIT_FILTER = "PwdReminderOrgUnitFilter";

	@Autowired
	private SettingDao settingDao;

	@Autowired
	private SofdConfiguration sofdConfiguration;

	public Set<String> getPwdReminderOrgUnitFilter() {
		String value = getKeyWithDefault(SETTING_PWD_REMINDER_ORGUNIT_FILTER, "");
		
		Set<String> result = new HashSet<>();
		String[] tokens = value.split(",");
		for (String token : tokens) {
			if (StringUtils.hasLength(token)) {
				result.add(token);
			}
		}

		return result;
	}
	
	public void setPwdReminderOrgUnitFilter(Set<String> value) {		
		Setting setting = settingDao.findByKey(SETTING_PWD_REMINDER_ORGUNIT_FILTER);
		if (setting == null) {
			setting = new Setting();
			setting.setKey(SETTING_PWD_REMINDER_ORGUNIT_FILTER);
		}

		StringBuilder builder = new StringBuilder();
		if (value != null) {
			for (String token : value) {
				if (builder.length() > 0) {
					builder.append(",");
				}
				
				builder.append(token);
			}
		}

		setting.setValue(builder.toString());
		settingDao.save(setting);
	}

	public String getPwdReminderDaysBefore() {
		return getKeyWithDefault(SETTING_PWD_REMINDER_DAYS_BEFORE, "14,7,3,1");
	}
	
	public void setPwdReminderDaysBefore(String value) {		
		Setting setting = settingDao.findByKey(SETTING_PWD_REMINDER_DAYS_BEFORE);
		if (setting == null) {
			setting = new Setting();
			setting.setKey(SETTING_PWD_REMINDER_DAYS_BEFORE);
		}

		Set<Long> days = new HashSet<>();
		String[] tokens = value.split(",");
		for (String token : tokens) {
			token = token.trim();
			
			try {
				long val = Long.valueOf(token);
				if (val > 0 && val < 30) {
					days.add(val);
				}
			}
			catch (Exception ex) {
				; // ignore
			}
		}

		StringBuilder builder = new StringBuilder();
		for (long day : days.stream().sorted().collect(Collectors.toSet())) {
			if (builder.length() > 0) {
				builder.append(",");
			}

			builder.append(Long.toString(day));
		}
		
		if (builder.length() == 0) {
			builder.append("14,7,3,1");
		}

		setting.setValue(builder.toString());
		settingDao.save(setting);
	}
	
	public LocalTime getPwdReminderTime() {
		String value = getKeyWithDefault(SETTING_PWD_REMINDER_TIME, "10:00");

		return LocalTime.parse(value);
	}
	
	public void setPwdReminderTime(LocalTime time) {		
		Setting setting = settingDao.findByKey(SETTING_PWD_REMINDER_TIME);
		if (setting == null) {
			setting = new Setting();
			setting.setKey(SETTING_PWD_REMINDER_TIME);
		}

		setting.setValue(time.toString());
		settingDao.save(setting);
	}
	
	public PwdReminderStrategy getPwdReminderStrategy() {
		String value = getKeyWithDefault(SETTING_PWD_REMINDER_STRATEGY, DISABLED.toString());

		return PwdReminderStrategy.valueOf(value);
	}

	public void setPwdReminderStrategy(PwdReminderStrategy strategy) {
		Setting setting = settingDao.findByKey(SETTING_PWD_REMINDER_STRATEGY);
		if (setting == null) {
			setting = new Setting();
			setting.setKey(SETTING_PWD_REMINDER_STRATEGY);
		}

		setting.setValue(strategy.toString());
		settingDao.save(setting);
	}

	public String getPwdReminderEmailTxt() {
		Setting setting = settingDao.findByKey(SETTING_PWD_REMINDER_EMAIL_TEXT);
		if (setting == null) {
			return "<strong>Kære {NAVN}</strong>.\n<br/><br/>\nDit kodeord til kontoen {KONTO} udløber om {DAGE} og skal skiftes inden da.\n<br/>\nMvh IT Afdelingen";
		}

		return setting.getValue();
	}

	public void setPwdReminderEmailTxt(String text) {
		Setting setting = settingDao.findByKey(SETTING_PWD_REMINDER_EMAIL_TEXT);
		if (setting == null) {
			setting = new Setting();
			setting.setKey(SETTING_PWD_REMINDER_EMAIL_TEXT);
		}

		setting.setValue(text);
		settingDao.save(setting);
	}

	public String getPwdReminderEmailSubject() {
		Setting setting = settingDao.findByKey(SETTING_PWD_REMINDER_EMAIL_SUBJECT);
		if (setting == null) {
			return "Påmindelse om kodeordsskifte";
		}

		return setting.getValue();
	}

	public void setPwdReminderEmailSubject(String subject) {
		Setting setting = settingDao.findByKey(SETTING_PWD_REMINDER_EMAIL_SUBJECT);
		if (setting == null) {
			setting = new Setting();
			setting.setKey(SETTING_PWD_REMINDER_EMAIL_SUBJECT);
		}

		setting.setValue(subject);
		settingDao.save(setting);
	}

	public String getPwdReminderSmsTxt() {
		Setting setting = settingDao.findByKey(SETTING_PWD_REMINDER_SMS_TEXT);
		if (setting == null) {
			return "Kære {NAVN}.\nDit kodeord til kontoen {KONTO} udløber om {DAGE} og skal skiftes inden da";
		}

		return setting.getValue();
	}

	public void setPwdReminderSmsTxt(String text) {
		Setting setting = settingDao.findByKey(SETTING_PWD_REMINDER_SMS_TEXT);
		if (setting == null) {
			setting = new Setting();
			setting.setKey(SETTING_PWD_REMINDER_SMS_TEXT);
		}

		setting.setValue(text);
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

	public PwdReminderStrategy[] getStrategyOptions() {
		if (!sofdConfiguration.getModules().getSmsGateway().isSmsEnabled()) {
			return new PwdReminderStrategy[]{DISABLED, EMAIL_ONLY};
		}
		return PwdReminderStrategy.values();
	}

}
