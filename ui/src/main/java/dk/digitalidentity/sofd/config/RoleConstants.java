package dk.digitalidentity.sofd.config;

public class RoleConstants {
	public static final String USER_ROLE_READ = "ROLE_USER_READ";      							// minimal access, can read data
	public static final String USER_ROLE_EDIT = "ROLE_USER_EDIT";      							// can edit data
	public static final String USER_ROLE_EDIT_CONTACT_INFO = "ROLE_USER_EDIT_CONTACT_INFO";     // can edit contact info on orgUnits
	public static final String USER_ROLE_ADMIN = "ROLE_USER_ADMIN";    							// full access, including admin pages
	public static final String USER_ROLE_LOS_ADMIN = "ROLE_USER_LOS_ADMIN";                     // same as EDIT + LOS access
	public static final String USER_ROLE_TELEPHONY = "ROLE_USER_TELEPHONY";                     // same as READ + Telephony access
	public static final String USER_ROLE_SMS = "ROLE_USER_SMS";       							// access to SMS module
	public static final String USER_ROLE_CPR_ACCESS = "ROLE_USER_CPR_ACCESS";					// shows full CPR numbers
	public static final String USER_ROLE_PERSON_CREATER = "ROLE_USER_PERSON_CREATER";			// allow creating persons from list.html
	public static final String USER_ROLE_CHART_EDITOR = "ROLE_USER_CHART_EDITOR";		    	// allow edtiting charts

	// module roles, given to all users, depending on global configuration
	public static final String MODULE_ROLE_ACCOUNT_CREATION = "ROLE_MODULE_ACCOUNT_CREATION";
	public static final String MODULE_ROLE_PROFILE = "ROLE_MODULE_PROFILE";
	public static final String MODULE_ROLE_SMS_GATEWAY = "ROLE_MODULE_SMS_GATEWAY";
	public static final String MODULE_ROLE_PERSON_COMMENT = "ROLE_MODULE_PERSON_COMMENT";
	public static final String MODULE_ROLE_LOS = "ROLE_MODULE_LOS";
	public static final String MODULE_ROLE_TELEPHONY = "ROLE_MODULE_TELEPHONY";

	// system-roles, used by API access
	public static final String SYSTEM_ROLE_WRITE_ACCESS = "ROLE_WRITE_ACCESS";
	public static final String SYSTEM_ROLE_READ_ACCESS = "ROLE_READ_ACCESS";
	public static final String SYSTEM_ROLE_LIMITED_READ_ACCESS = "ROLE_LIMITED_READ_ACCESS";
	
	// roles given due to data in SOFD at login time
	public static final String DATA_ROLE_MANAGER = "ROLE_MANAGER";
}
