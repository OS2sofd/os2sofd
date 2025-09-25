package dk.digitalidentity.sofd.dao.model.enums;

import lombok.Getter;

@Getter
public enum EmailOrgUnitFilterType {
	INCLUDE("html.entity.email_template.orgunit_filter.include"),
	INCLUDE_INHERIT("html.entity.email_template.orgunit_filter.include_inherit"),
	EXCLUDE("html.entity.email_template.orgunit_filter.exclude"),
	EXCLUDE_INHERIT("html.entity.email_template.orgunit_filter.exclude_inherit");

	private String title;

	private EmailOrgUnitFilterType(String title) {
		this.title = title;
	}
}
