package dk.digitalidentity.sofd.service;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import dk.digitalidentity.sofd.dao.SupportedUserTypeDao;
import dk.digitalidentity.sofd.dao.model.SupportedUserType;

@Service
@EnableCaching
@EnableScheduling
public class SupportedUserTypeService {

	// this list should be kept, even when we have the database values,
	// as these are the UserTypes that the code deals with in a special way
	private static enum WellKnownUserType {
		ACTIVE_DIRECTORY, OPUS, UNILOGIN, EXCHANGE, KSPCICS, ACTIVE_DIRECTORY_AND_EXCHANGE, SCHOOL_EMAIL, MITID_ERHVERV, ACTIVE_DIRECTORY_SCHOOL, AZURE_AD
	}

	@Autowired
	private SupportedUserTypeDao supportedUserTypeDao;

	@Autowired
	private SupportedUserTypeService self;

	public SupportedUserType findById(long id) {
		return supportedUserTypeDao.findById(id);
	}

	@Cacheable(value = "userTypeCache")
	public SupportedUserType findByKey(String key) {
		return supportedUserTypeDao.findByKey(key);
	}

	public SupportedUserType save(SupportedUserType userType) {
		return supportedUserTypeDao.save(userType);
	}

	@Cacheable(value = "allUserTypes")
	public List<SupportedUserType> findAll() {
		return supportedUserTypeDao.findAll();
	}
	
	public Collection<SupportedUserType> findAllBypassCache() {
		return supportedUserTypeDao.findAll();
	}
	
	// run every 30 seconds
	@Scheduled(fixedRate = 1000 * 30)
	public void cacheClearTask() {
		self.cacheClear();
	}

	@CacheEvict(value = { "allUserTypes", "userTypeCache" }, allEntries = true)
	public void cacheClear() {
		; // do nothing, annotation handles actual logic
	}
	
	public boolean isValidUserType(String userType) {
		SupportedUserType supportedUserType = self.findByKey(userType);
		if (supportedUserType != null) {
			return true;
		}

		return false;
	}

	public String getPrettyName(String userType) {
		SupportedUserType supportedUserType = self.findByKey(userType);
		if (supportedUserType != null) {
			return supportedUserType.getName();
		}

		return "Ukendt";
	}

	public List<String> getAllUserTypes() {
		return findAll().stream()
				.map(SupportedUserType::getKey)
				.collect(Collectors.toList());
	}

	public static boolean isActiveDirectory(String userType) {
		return (WellKnownUserType.ACTIVE_DIRECTORY.toString().equals(userType));
	}

	public static boolean isAzureAd(String userType) {
		return (WellKnownUserType.AZURE_AD.toString().equals(userType));
	}

	public static boolean isActiveDirectorySchool(String userType) {
		return (WellKnownUserType.ACTIVE_DIRECTORY_SCHOOL.toString().equals(userType));
	}

	public static boolean isMitIDErhverv(String userType) {
		return (WellKnownUserType.MITID_ERHVERV.toString().equals(userType));
	}

	public static boolean isExchange(String userType) {
		return (WellKnownUserType.EXCHANGE.toString().equals(userType));
	}

	public static boolean isOpus(String userType) {
		return (WellKnownUserType.OPUS.toString().equals(userType));
	}
	
	public static boolean isKspCics(String userType) {
		return (WellKnownUserType.KSPCICS.toString().equals(userType));
	}

	public static boolean isUniLogin(String userType) {
		return (WellKnownUserType.UNILOGIN.toString().equals(userType));
	}
	
	public static boolean isActiveDirectoryAndExchange(String userType) {
		return (WellKnownUserType.ACTIVE_DIRECTORY_AND_EXCHANGE.toString().equals(userType));
	}

	public static boolean isSchoolEmail(String userType) {
		return (WellKnownUserType.SCHOOL_EMAIL.toString().equals(userType));
	}

	public static String getMitIDErhvervUserType() {
		return WellKnownUserType.MITID_ERHVERV.toString();
	}

	public static String getActiveDirectoryUserType() {
		return WellKnownUserType.ACTIVE_DIRECTORY.toString();
	}

	public static String getOpusUserType() {
		return WellKnownUserType.OPUS.toString();
	}
	
	public static String getExchangeUserType() {
		return WellKnownUserType.EXCHANGE.toString();
	}

	public static String getUniLoginUserType() {
		return WellKnownUserType.UNILOGIN.toString();
	}
	
	public static String getAzureAdUserType() {
		return WellKnownUserType.AZURE_AD.toString();
	}

    public static String getKspCicsUserType() { return WellKnownUserType.KSPCICS.toString(); }
}
