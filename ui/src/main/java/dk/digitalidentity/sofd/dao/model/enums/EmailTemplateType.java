package dk.digitalidentity.sofd.dao.model.enums;

import java.util.Arrays;
import java.util.List;

import lombok.Getter;

@Getter
public enum EmailTemplateType {
	AD_CREATE_EMPLOYEE("html.enum.email.message.type.ad_create_employee", Arrays.asList(Flags.REQUIRE_ACCOUNT_CREATION, Flags.EBOKS)),
	EXCHANGE_CREATE_EMPLOYEE_EBOKS("html.enum.email.message.type.exchange_create_employee_eboks", Arrays.asList(Flags.REQUIRE_ACCOUNT_CREATION, Flags.EBOKS)),
	AD_CREATE_FAILED("html.enum.email.message.type.ad_create_failed", Arrays.asList(Flags.REQUIRE_ACCOUNT_CREATION, Flags.SHOW_CC)),
	EXCHANGE_CREATE_EMPLOYEE("html.enum.email.message.type.exchange_create_employee", Arrays.asList(Flags.REQUIRE_ACCOUNT_CREATION, Flags.SHOW_CC)),
	EXCHANGE_CREATE_EMPLOYEE2("html.enum.email.message.type.exchange_create_employee2", Arrays.asList(Flags.REQUIRE_ACCOUNT_CREATION, Flags.SHOW_CC)),
	AD_CREATE_MANAGER("html.enum.email.message.type.ad_create_manager", Arrays.asList(Flags.REQUIRE_ACCOUNT_CREATION, Flags.SHOW_CC, Flags.SEND_TO_MANAGER)),
	AD_DISABLE_MANAGER("html.enum.email.message.type.ad_disable_manager", Arrays.asList(Flags.REQUIRE_ACCOUNT_CREATION, Flags.SHOW_CC, Flags.SEND_TO_MANAGER)),
	
	NEW_AFFILIATION("html.enum.email.message.type.new_affiliation", Arrays.asList(Flags.SHOW_CC, Flags.SEND_TO_MANAGER, Flags.SHOW_ORG_FILTER)),
	NEW_EMPLOYEE_WELCOME("html.enum.email.message.type.new_employee_welcome", Arrays.asList(Flags.SHOW_CC, Flags.SEND_TO_MANAGER, Flags.SHOW_DOMAIN_FILTER, Flags.SHOW_ORG_FILTER)),
	NEW_EMPLOYEE_REMINDER("html.enum.email.message.type.new_employee_reminder", Arrays.asList(Flags.SHOW_CC, Flags.SEND_TO_MANAGER, Flags.SHOW_DOMAIN_FILTER, Flags.SHOW_ORG_FILTER)),
	AFFILIATION_EXPIRE_REMINDER("html.enum.email.message.type.affiliation_expire_reminder", Arrays.asList(Flags.SHOW_CC, Flags.SEND_TO_MANAGER, Flags.ALLOW_DAYS_BEFORE_EVENT, Flags.SHOW_ORG_FILTER)),
	RESIGNATION("html.enum.email.message.type.resignation", Arrays.asList(Flags.SHOW_CC, Flags.SEND_TO_MANAGER, Flags.ALLOW_DAYS_BEFORE_EVENT, Flags.SHOW_ORG_FILTER, Flags.SHOW_AD_USER_FILTER)),

	ORGUNIT_CHANGES("html.enum.email.message.type.orgunit_changes", Arrays.asList(Flags.SHOW_TO, Flags.SHOW_CC)),
	
	SUBSTITUTE_STOPS("html.enum.email.message.type.substitute_stops", Arrays.asList(Flags.SHOW_CC)),
	
	NEW_MANAGER("html.enum.email.message.type.new_manager", Arrays.asList(Flags.SHOW_TO, Flags.SHOW_CC, Flags.SHOW_ORG_FILTER)),
	MANAGER_REMOVED("html.enum.email.message.type.manager_removed", Arrays.asList(Flags.SHOW_TO, Flags.SHOW_CC, Flags.SHOW_ORG_FILTER)),
	MANAGER_STOPS("html.enum.email.message.type.manager_stops", Arrays.asList(Flags.SHOW_TO, Flags.SHOW_CC)),

	FUNCTION_NEW_MANAGER("html.enum.email.message.type.function_new_manager", Arrays.asList(Flags.REQUIRE_FUNCTION_HIERARCHY)),
	FUNCTION_ASSIGNMENT_EXPIRES("html.enum.email.message.type.function_assignment_expires", Arrays.asList(Flags.REQUIRE_FUNCTION_HIERARCHY));

	
	private String message;
	private boolean requireAccountCreation;
	private boolean showTO;
	private boolean showCC;
	private boolean sendToManager;
	private boolean eboks;
	private boolean allowDaysBeforeEvent;
	private boolean showDomainFilter;
	private boolean showOrgFilter;
	private boolean showADUserFilter;
	private boolean requireFunctionHierarchy;

	private EmailTemplateType(String message, List<Flags> flags) {
		this.message = message;
		this.requireAccountCreation = flags.contains(Flags.REQUIRE_ACCOUNT_CREATION);
		this.showTO = flags.contains(Flags.SHOW_TO);
		this.showCC = flags.contains(Flags.SHOW_CC);
		this.sendToManager = flags.contains(Flags.SEND_TO_MANAGER);
		this.eboks = flags.contains(Flags.EBOKS);
		this.allowDaysBeforeEvent = flags.contains(Flags.ALLOW_DAYS_BEFORE_EVENT);
		this.showDomainFilter = flags.contains(Flags.SHOW_DOMAIN_FILTER);
		this.showOrgFilter = flags.contains(Flags.SHOW_ORG_FILTER);
		this.showADUserFilter = flags.contains(Flags.SHOW_AD_USER_FILTER);
		this.requireFunctionHierarchy = flags.contains(Flags.REQUIRE_FUNCTION_HIERARCHY);
	}
	
	private enum Flags { REQUIRE_ACCOUNT_CREATION, SHOW_TO, SHOW_CC, SEND_TO_MANAGER, EBOKS, ALLOW_DAYS_BEFORE_EVENT, SHOW_DOMAIN_FILTER, SHOW_ORG_FILTER, SHOW_AD_USER_FILTER, REQUIRE_FUNCTION_HIERARCHY };
}
