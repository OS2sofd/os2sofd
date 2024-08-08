package dk.digitalidentity.sofd.dao.model.enums;

import lombok.Getter;

@Getter
public enum EmailTemplatePlaceholder {
	EMPLOYEE_PLACEHOLDER("{medarbejder}", "html.enum.placeholders.description.employee"),
	AFFILIATIONUUID_PLACEHOLDER("{tilhørsforholduuid}", "html.enum.placeholders.description.affiliation_uuid"),
	ORGUNIT_PLACEHOLDER("{orgenhed}", "html.enum.placeholders.description.orgunit"),
	ACCOUNT_PLACEHOLDER("{kontonavn}", "html.enum.placeholders.description.account"),
	RECEIVER_PLACEHOLDER("{modtager}", "html.enum.placeholders.description.receiver"),
	TIMESTAMP_PLACEHOLDER("{tidspunkt}", "html.enum.placeholders.description.timestamp"),
	CHANGES_PLACEHOLDER("{ændringer}", "html.enum.placeholders.description.changes"),
	EXCHANGE_ACCOUNT_PLACEHOLDER("{exchange}", "html.enum.placeholders.description.exchange_account"),
	VENDOR_PLACEHOLDER("{leverandør}", "html.enum.placeholders.description.vendor"),
	INTERNAL_REFERENCE_PLACEHOLDER("{intern reference}", "html.enum.placeholders.description.internal_reference"),
	MANAGER_PLACEHOLDER("{leder}", "html.enum.placeholders.description.manager"),
	SUBSTITUTE_PLACEHOLDER("{stedfortræder}", "html.enum.placeholders.description.substitute"),
	SUBSTITUTE_CONTEXT_PLACEHOLDER("{kontekst}", "html.enum.placeholders.description.substitute_context"),
	DAYS_BEFORE_EVENT("{dage}", "html.enum.placeholders.description.days_before_event"),
	POSITION_NAME_PLACEHOLDER("{stilling}", "html.enum.placeholders.description.position_name"),
	EMPLOYEE_NUMBER_PLACEHOLDER("{medarbejdernummer}", "html.enum.placeholders.description.employee_number"),
	FUNCTION_NAME("{funktion}", "html.enum.placeholders.description.function_name"),
	FUNCTION_LIST("{funktionsliste}", "html.enum.placeholders.description.function_list"),
	SUBSTITUTE_LIST("{stedfortrædere}", "html.enum.placeholders.description.substitute_list"),
	UPN("{upn}", "html.enum.placeholders.description.upn");
	
	private String placeholder;
	private String description;
	
	private EmailTemplatePlaceholder(String placeholder, String description) {
		this.placeholder = placeholder;
		this.description = description;
	}
}
