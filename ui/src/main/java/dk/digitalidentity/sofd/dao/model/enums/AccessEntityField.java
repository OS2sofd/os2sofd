package dk.digitalidentity.sofd.dao.model.enums;

import java.util.ArrayList;
import java.util.List;

public enum AccessEntityField {
	ORGUNIT_BASIC("html.enum.accessEntityfield.orgunit.basic.label", "html.enum.accessEntityfield.orgunit.basic.description"),
	ORGUNIT_ADDRESS("html.enum.accessEntityfield.orgunit.address.label", "html.enum.accessEntityfield.orgunit.address.description"),
	ORGUNIT_MANAGER("html.enum.accessEntityfield.orgunit.manager.label", "html.enum.accessEntityfield.orgunit.manager.description"),
	ORGUNIT_KLE("html.enum.accessEntityfield.orgunit.kle.label", "html.enum.accessEntityfield.orgunit.kle.description"),
	ORGUNIT_AFFILIATIONS("html.enum.accessEntityfield.orgunit.affiliations.label", "html.enum.accessEntityfield.orgunit.affiliations.description"),
	ORGUNIT_AFFILIATIONS_DETAILS("html.enum.accessEntityfield.orgunit.affiliations_details.label", "html.enum.accessEntityfield.orgunit.affiliations_details.description"),
	ORGUNIT_PHONE("html.enum.accessEntityfield.orgunit.phone.label", "html.enum.accessEntityfield.orgunit.phone.description"),

	PERSON_BASIC("html.enum.accessEntityfield.person.basic.label", "html.enum.accessEntityfield.person.basic.description"),
	PERSON_CPR("html.enum.accessEntityfield.person.cpr.label", "html.enum.accessEntityfield.person.cpr.description"),
	PERSON_ADDRESS("html.enum.accessEntityfield.person.address.label", "html.enum.accessEntityfield.person.address.description"),
	PERSON_USER("html.enum.accessEntityfield.person.user.label", "html.enum.accessEntityfield.person.user.description"),
	PERSON_AFFILIATIONS("html.enum.accessEntityfield.person.affiliations.label", "html.enum.accessEntityfield.person.affiliations.description"),
	PERSON_AFFILIATIONS_DETAILS("html.enum.accessEntityfield.person.affiliations_details.label", "html.enum.accessEntityfield.person.affiliations_details.description"),
	PERSON_PHONE("html.enum.accessEntityfield.person.phone.label", "html.enum.accessEntityfield.person.phone.description");

	private String messageId;
	private String description;
	private static List<AccessEntityField> personFields;
	private static List<AccessEntityField> orgunitFields;

	static {
		personFields = new ArrayList<>();
		orgunitFields = new ArrayList<>();

		for (AccessEntityField field : AccessEntityField.values()) {
			if (field.name().startsWith("ORGUNIT_")) {
				orgunitFields.add(field);
			}
			else if (field.name().startsWith("PERSON_")) {
				personFields.add(field);
			}
		}
	}

	private AccessEntityField(String messageId, String description) {
		this.messageId = messageId;
		this.description = description;
	}

	public static List<AccessEntityField> getAllPersonFields() {
		return personFields;
	}

	public static List<AccessEntityField> getAllOrgunitFields() {
		return orgunitFields;
	}

	public String getMessageId() {
		return messageId;
	}

	public String getDescription(){
		return description;
	}
}
