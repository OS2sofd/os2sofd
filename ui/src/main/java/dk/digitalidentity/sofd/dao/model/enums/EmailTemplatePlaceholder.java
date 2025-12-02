package dk.digitalidentity.sofd.dao.model.enums;

import lombok.Getter;

@Getter
public enum EmailTemplatePlaceholder {
	EMPLOYEE_PLACEHOLDER("{medarbejder}", "html.enum.placeholders.description.employee", "html.enum.placeholders.description.employee.example"),
	AFFILIATIONUUID_PLACEHOLDER("{tilhørsforholduuid}", "html.enum.placeholders.description.affiliation_uuid", "html.enum.placeholders.description.affiliation_uuid.example"),
	ORGUNIT_PLACEHOLDER("{orgenhed}", "html.enum.placeholders.description.orgunit", "html.enum.placeholders.description.orgunit.example"),
	ACCOUNT_PLACEHOLDER("{ad-brugernavn}", "html.enum.placeholders.description.account", "html.enum.placeholders.description.account.example"),
	RECEIVER_PLACEHOLDER("{modtager}", "html.enum.placeholders.description.receiver", "html.enum.placeholders.description.receiver.example"),
	TIMESTAMP_PLACEHOLDER("{tidspunkt}", "html.enum.placeholders.description.timestamp", "html.enum.placeholders.description.timestamp.example"),
	CHANGES_PLACEHOLDER("{ændringer}", "html.enum.placeholders.description.changes", "html.enum.placeholders.description.changes.example"),
	EXCHANGE_ACCOUNT_PLACEHOLDER("{exchange-email}", "html.enum.placeholders.description.exchange_account", "html.enum.placeholders.description.exchange_account.example"),
	VENDOR_PLACEHOLDER("{leverandør}", "html.enum.placeholders.description.vendor", "html.enum.placeholders.description.vendor.example"),
	INTERNAL_REFERENCE_PLACEHOLDER("{intern reference}", "html.enum.placeholders.description.internal_reference", "html.enum.placeholders.description.internal_reference.example"),
	MANAGER_PLACEHOLDER("{leder}", "html.enum.placeholders.description.manager", "html.enum.placeholders.description.manager.example"),
	SUBSTITUTE_PLACEHOLDER("{stedfortræder}", "html.enum.placeholders.description.substitute", "html.enum.placeholders.description.substitute.example"),
	SUBSTITUTE_CONTEXT_PLACEHOLDER("{kontekst}", "html.enum.placeholders.description.substitute_context", "html.enum.placeholders.description.substitute_context.example"),
	DAYS_BEFORE_EVENT("{dage}", "html.enum.placeholders.description.days_before_event", "html.enum.placeholders.description.days_before_event.example"),
	POSITION_NAME_PLACEHOLDER("{stilling}", "html.enum.placeholders.description.position_name", "html.enum.placeholders.description.position_name.example"),
	EMPLOYEE_NUMBER_PLACEHOLDER("{medarbejdernummer}", "html.enum.placeholders.description.employee_number", "html.enum.placeholders.description.employee_number.example"),
	FUNCTION_NAME("{funktion}", "html.enum.placeholders.description.function_name", "html.enum.placeholders.description.function_name.example"),
	FUNCTION_LIST("{funktionsliste}", "html.enum.placeholders.description.function_list", "html.enum.placeholders.description.function_list.example"),
	SUBSTITUTE_LIST("{stedfortrædere}", "html.enum.placeholders.description.substitute_list", "html.enum.placeholders.description.substitute_list.example"),
	UPN("{upn}", "html.enum.placeholders.description.upn", "html.enum.placeholders.description.upn.example"),
	TITLE("{overskrift}", "html.enum.placeholders.description.title", "html.enum.placeholders.description.title.example"),
	PRIMARY_ACCOUNT_PLACEHOLDER("{primærekonto}", "html.enum.placeholders.description.primary_account","html.enum.placeholders.description.primary_account.example"),
	ALL_ACCOUNTS_PLACEHOLDER("{konti}", "html.enum.placeholders.description.all_accounts","html.enum.placeholders.description.all_accounts.example"),
    CICS_ACCOUNT_PLACEHOLDER("{cics-brugernavn}", "html.enum.placeholders.description.cics_account", "html.enum.placeholders.description.cics_account.example");

	private String placeholder;
	private String description;
	private String example;
	
	private EmailTemplatePlaceholder(String placeholder, String description, String example) {
		this.placeholder = placeholder;
		this.description = description;
		this.example = example;
	}
}
