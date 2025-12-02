package dk.digitalidentity.sofd.dao.model.enums;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;

@Getter
public enum EmailTemplateType {
	AD_CREATE_EMPLOYEE("html.enum.email.message.type.ad_create_employee", Arrays.asList(Flags.EBOKS, Flags.SHOW_EMPLOYEE_FILTER, Flags.SHOW_ORG_FILTER), Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.ACCOUNT_PLACEHOLDER, EmailTemplatePlaceholder.TITLE )),
	AD_CREATE_SUBSTITUTE("html.enum.email.message.type.ad_create_substitute", Arrays.asList(Flags.REQUIRE_SUBSTITUTE, Flags.EBOKS), Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.ACCOUNT_PLACEHOLDER, EmailTemplatePlaceholder.TITLE)),
	EXCHANGE_CREATE_EMPLOYEE_EBOKS("html.enum.email.message.type.exchange_create_employee_eboks", Arrays.asList(Flags.REQUIRE_ACCOUNT_CREATION, Flags.EBOKS, Flags.SHOW_EMPLOYEE_FILTER, Flags.SHOW_ORG_FILTER), Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER, EmailTemplatePlaceholder.ACCOUNT_PLACEHOLDER, EmailTemplatePlaceholder.EXCHANGE_ACCOUNT_PLACEHOLDER, EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER, EmailTemplatePlaceholder.TITLE)),
	// todo consider renaming this to ACCOUNT_ORDER_FAILED - remember to migrate existing data
	AD_CREATE_FAILED("html.enum.email.message.type.ad_create_failed", Arrays.asList(Flags.REQUIRE_ACCOUNT_CREATION, Flags.SHOW_CC,Flags.SHOW_TO), Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER)),
	EXCHANGE_CREATE_EMPLOYEE("html.enum.email.message.type.exchange_create_employee", Arrays.asList(Flags.REQUIRE_ACCOUNT_CREATION, Flags.SHOW_CC, Flags.SHOW_EMPLOYEE_FILTER, Flags.SHOW_ORG_FILTER,Flags.SHOW_TO), Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER, EmailTemplatePlaceholder.ACCOUNT_PLACEHOLDER, EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER, EmailTemplatePlaceholder.EXCHANGE_ACCOUNT_PLACEHOLDER)),
	EXCHANGE_CREATE_EMPLOYEE2("html.enum.email.message.type.exchange_create_employee2", Arrays.asList(Flags.REQUIRE_ACCOUNT_CREATION, Flags.SHOW_CC, Flags.SHOW_EMPLOYEE_FILTER, Flags.SHOW_ORG_FILTER,Flags.SHOW_TO), Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER, EmailTemplatePlaceholder.ACCOUNT_PLACEHOLDER, EmailTemplatePlaceholder.EXCHANGE_ACCOUNT_PLACEHOLDER)),
	EXCHANGE_CREATE_MANAGER("html.enum.email.message.type.exchange_create_manager", Arrays.asList(Flags.REQUIRE_ACCOUNT_CREATION, Flags.SHOW_CC, Flags.SEND_TO_MANAGER, Flags.SHOW_ORG_FILTER,Flags.SHOW_TO), Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER, EmailTemplatePlaceholder.ACCOUNT_PLACEHOLDER, EmailTemplatePlaceholder.UPN, EmailTemplatePlaceholder.EXCHANGE_ACCOUNT_PLACEHOLDER)),
	AD_CREATE_MANAGER("html.enum.email.message.type.ad_create_manager", Arrays.asList(Flags.REQUIRE_ACCOUNT_CREATION, Flags.SHOW_CC, Flags.SEND_TO_MANAGER, Flags.SHOW_ORG_FILTER,Flags.SHOW_TO), Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER, EmailTemplatePlaceholder.ACCOUNT_PLACEHOLDER, EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER)),
	AD_DISABLE_MANAGER("html.enum.email.message.type.ad_disable_manager", Arrays.asList(Flags.REQUIRE_ACCOUNT_CREATION, Flags.SHOW_CC, Flags.SEND_TO_MANAGER,Flags.SHOW_TO), Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER, EmailTemplatePlaceholder.ACCOUNT_PLACEHOLDER, EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER)),
	AD_DELETE_MANAGER("html.enum.email.message.type.ad_delete_manager", Arrays.asList(Flags.REQUIRE_ACCOUNT_CREATION, Flags.SHOW_CC, Flags.SHOW_ORG_FILTER, Flags.SEND_TO_MANAGER,Flags.SHOW_TO), Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER, EmailTemplatePlaceholder.ACCOUNT_PLACEHOLDER, EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER)),
	AD_REACTIVATED("html.enum.email.message.type.ad_reactivated", Arrays.asList(Flags.REQUIRE_ACCOUNT_CREATION, Flags.EBOKS, Flags.SHOW_EMPLOYEE_FILTER, Flags.SHOW_ORG_FILTER), Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.ACCOUNT_PLACEHOLDER, EmailTemplatePlaceholder.TITLE)),
	CICS_CREATE_EMPLOYEE("html.enum.email.message.type.cics_create_employee", Arrays.asList(Flags.EBOKS, Flags.SHOW_EMPLOYEE_FILTER, Flags.SHOW_ORG_FILTER), Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.CICS_ACCOUNT_PLACEHOLDER, EmailTemplatePlaceholder.TITLE)),
	CICS_CREATE_MANAGER("html.enum.email.message.type.cics_create_manager", Arrays.asList(Flags.REQUIRE_ACCOUNT_CREATION, Flags.SHOW_CC, Flags.SEND_TO_MANAGER, Flags.SHOW_ORG_FILTER,Flags.SHOW_TO), Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER, EmailTemplatePlaceholder.CICS_ACCOUNT_PLACEHOLDER, EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER)),
    // This is what im supposed to copy from
	ORDER_PENDING_APPOVAL("html.enum.email.message.type.new_account_pending_approval", Arrays.asList(Flags.REQUIRE_ACCOUNT_CREATION, Flags.REQUIRE_ACCOUNT_CREATION_APPROVAL, Flags.SEND_TO_MANAGER,Flags.SHOW_TO), Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER, EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER)),
	// This is the one to copy to
	NEW_AFFILIATION("html.enum.email.message.type.new_affiliation", Arrays.asList(Flags.SHOW_CC, Flags.SEND_TO_MANAGER, Flags.SHOW_ORG_FILTER,Flags.SHOW_TO), Arrays.asList(EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER, EmailTemplatePlaceholder.AFFILIATIONUUID_PLACEHOLDER, EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER, EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.TIMESTAMP_PLACEHOLDER, EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER)),
	NEW_AFFILIATION_SOFD("html.enum.email.message.type.new_affiliation_sofd", Arrays.asList(Flags.SHOW_CC, Flags.SEND_TO_MANAGER, Flags.SHOW_ORG_FILTER,Flags.SHOW_TO), Arrays.asList(EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER, EmailTemplatePlaceholder.AFFILIATIONUUID_PLACEHOLDER, EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER, EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.TIMESTAMP_PLACEHOLDER, EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER)),
	
	PAUSE_MARKING_ENDED("html.enum.email.message.type.pause_marking_ended", Arrays.asList(Flags.REQUIRE_ACCOUNT_CREATION, Flags.EBOKS, Flags.SHOW_AD_USER_FILTER), Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.PRIMARY_ACCOUNT_PLACEHOLDER, EmailTemplatePlaceholder.ALL_ACCOUNTS_PLACEHOLDER)),

	NEW_EMPLOYEE_DIGITAL_POST("html.enum.email.message.type.new_employee_digital_post", Arrays.asList(Flags.EBOKS, Flags.SHOW_ORG_FILTER), Arrays.asList(EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER, EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER, EmailTemplatePlaceholder.TITLE)),
	NEW_EMPLOYEE_WELCOME("html.enum.email.message.type.new_employee_welcome", Arrays.asList(Flags.SHOW_CC, Flags.SEND_TO_MANAGER, Flags.SHOW_DOMAIN_FILTER, Flags.SHOW_ORG_FILTER,Flags.SHOW_TO), Arrays.asList(EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER, EmailTemplatePlaceholder.MANAGER_PLACEHOLDER, EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER)),
	NEW_EMPLOYEE_REMINDER("html.enum.email.message.type.new_employee_reminder", Arrays.asList(Flags.SHOW_CC, Flags.SEND_TO_MANAGER, Flags.SHOW_DOMAIN_FILTER, Flags.SHOW_ORG_FILTER,Flags.SHOW_TO), Arrays.asList(EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER, EmailTemplatePlaceholder.MANAGER_PLACEHOLDER, EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER)),
	AFFILIATION_EXPIRE_REMINDER("html.enum.email.message.type.affiliation_expire_reminder", Arrays.asList(Flags.SHOW_CC, Flags.SEND_TO_MANAGER, Flags.ALLOW_DAYS_BEFORE_EVENT, Flags.SHOW_ORG_FILTER,Flags.SHOW_TO), Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER, EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER, EmailTemplatePlaceholder.TIMESTAMP_PLACEHOLDER, EmailTemplatePlaceholder.VENDOR_PLACEHOLDER, EmailTemplatePlaceholder.INTERNAL_REFERENCE_PLACEHOLDER, EmailTemplatePlaceholder.DAYS_BEFORE_EVENT, EmailTemplatePlaceholder.POSITION_NAME_PLACEHOLDER, EmailTemplatePlaceholder.EMPLOYEE_NUMBER_PLACEHOLDER, EmailTemplatePlaceholder.ACCOUNT_PLACEHOLDER)),
	RESIGNATION("html.enum.email.message.type.resignation", Arrays.asList(Flags.SHOW_CC, Flags.SEND_TO_MANAGER, Flags.ALLOW_DAYS_BEFORE_EVENT, Flags.SHOW_ORG_FILTER, Flags.SHOW_AD_USER_FILTER,Flags.SHOW_TO), Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER, EmailTemplatePlaceholder.TIMESTAMP_PLACEHOLDER, EmailTemplatePlaceholder.AFFILIATIONUUID_PLACEHOLDER, EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER, EmailTemplatePlaceholder.VENDOR_PLACEHOLDER, EmailTemplatePlaceholder.INTERNAL_REFERENCE_PLACEHOLDER, EmailTemplatePlaceholder.DAYS_BEFORE_EVENT, EmailTemplatePlaceholder.POSITION_NAME_PLACEHOLDER, EmailTemplatePlaceholder.EMPLOYEE_NUMBER_PLACEHOLDER, EmailTemplatePlaceholder.ACCOUNT_PLACEHOLDER)),

	ORGUNIT_CHANGES("html.enum.email.message.type.orgunit_changes", Arrays.asList(Flags.SHOW_TO, Flags.SHOW_CC), Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.CHANGES_PLACEHOLDER)),
	
	SUBSTITUTE_STOPS("html.enum.email.message.type.substitute_stops", Arrays.asList(Flags.SHOW_CC,Flags.ALLOW_DAYS_BEFORE_EVENT,Flags.SHOW_TO), Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.SUBSTITUTE_PLACEHOLDER, EmailTemplatePlaceholder.SUBSTITUTE_CONTEXT_PLACEHOLDER, EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER)),
	
	NEW_MANAGER("html.enum.email.message.type.new_manager", Arrays.asList(Flags.SHOW_CC, Flags.SHOW_ORG_FILTER, Flags.SHOW_TO, Flags.SEND_TO_MANAGER), Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER, EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER, EmailTemplatePlaceholder.SUBSTITUTE_LIST)),
	MANAGER_REMOVED("html.enum.email.message.type.manager_removed", Arrays.asList(Flags.SHOW_CC, Flags.SHOW_ORG_FILTER,Flags.SHOW_TO), Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER, EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER)),
	MANAGER_STOPS("html.enum.email.message.type.manager_stops", Arrays.asList(Flags.SHOW_CC,Flags.SHOW_TO), Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER, EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER)),

	FUNCTION_NEW_MANAGER("html.enum.email.message.type.function_new_manager", Arrays.asList(Flags.REQUIRE_FUNCTION_HIERARCHY, Flags.SHOW_TO), Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.FUNCTION_LIST)),
	FUNCTION_ASSIGNMENT_EXPIRES("html.enum.email.message.type.function_assignment_expires", Arrays.asList(Flags.REQUIRE_FUNCTION_HIERARCHY,Flags.SHOW_TO), Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.DAYS_BEFORE_EVENT, EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER, EmailTemplatePlaceholder.FUNCTION_NAME, EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER)),
	FUNCTION_ASSIGNMENT_FOLLOW_UP("html.enum.email.message.type.function_assignment_follow_up", Arrays.asList(Flags.REQUIRE_FUNCTION_HIERARCHY,Flags.SHOW_TO), Arrays.asList(EmailTemplatePlaceholder.RECEIVER_PLACEHOLDER, EmailTemplatePlaceholder.EMPLOYEE_PLACEHOLDER, EmailTemplatePlaceholder.FUNCTION_NAME, EmailTemplatePlaceholder.ORGUNIT_PLACEHOLDER));
	
	private String message;
	private boolean requireAccountCreation;
	private boolean requireAccountCreationApproval;
	private boolean requireSubstitute;
	private boolean showTO;
	private boolean showCC;
	private boolean sendToManager;
	private boolean eboks;
	private boolean allowDaysBeforeEvent;
	private boolean showEmployeeFilter;
	private boolean showDomainFilter;
	private boolean showOrgFilter;
	private boolean showADUserFilter;
	private boolean requireFunctionHierarchy;
	private List<EmailTemplatePlaceholder> emailTemplatePlaceholders;

	private EmailTemplateType(String message, List<Flags> flags, List<EmailTemplatePlaceholder> placeholders) {
		this.message = message;
		this.requireAccountCreation = flags.contains(Flags.REQUIRE_ACCOUNT_CREATION);
		this.requireAccountCreationApproval = flags.contains(Flags.REQUIRE_ACCOUNT_CREATION_APPROVAL);
		this.requireSubstitute = flags.contains(Flags.REQUIRE_SUBSTITUTE);
		this.showTO = flags.contains(Flags.SHOW_TO);
		this.showCC = flags.contains(Flags.SHOW_CC);
		this.sendToManager = flags.contains(Flags.SEND_TO_MANAGER);
		this.eboks = flags.contains(Flags.EBOKS);
		this.allowDaysBeforeEvent = flags.contains(Flags.ALLOW_DAYS_BEFORE_EVENT);
		this.showEmployeeFilter = flags.contains(Flags.SHOW_EMPLOYEE_FILTER);
		this.showDomainFilter = flags.contains(Flags.SHOW_DOMAIN_FILTER);
		this.showOrgFilter = flags.contains(Flags.SHOW_ORG_FILTER);
		this.showADUserFilter = flags.contains(Flags.SHOW_AD_USER_FILTER);
		this.requireFunctionHierarchy = flags.contains(Flags.REQUIRE_FUNCTION_HIERARCHY);
		this.emailTemplatePlaceholders = placeholders;
	}
	
	private enum Flags {
		// add these flags to only show the template if the given feature is enabled
		REQUIRE_ACCOUNT_CREATION,
		REQUIRE_ACCOUNT_CREATION_APPROVAL,
		REQUIRE_FUNCTION_HIERARCHY,
		REQUIRE_SUBSTITUTE,

		// enable various sending-variant flags
		SHOW_TO,
		SHOW_CC,
		SEND_TO_MANAGER,
		EBOKS,
		
		// special case, to show a delay option for sending
		ALLOW_DAYS_BEFORE_EVENT,
		
		// enable various filters
		SHOW_DOMAIN_FILTER,
		SHOW_ORG_FILTER,
		SHOW_AD_USER_FILTER,
		SHOW_EMPLOYEE_FILTER
	};
}
