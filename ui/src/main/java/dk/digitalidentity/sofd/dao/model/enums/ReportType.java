package dk.digitalidentity.sofd.dao.model.enums;

import lombok.Getter;

@Getter
public enum ReportType {
	AD_ACCOUNT_BUT_NO_AFFILIATION("html.report.ad_but_no_affiliation"),
	AD_ACCOUNT_BUT_NO_WAGES_AFFILIATION("html.report.ad_but_no_wages_affiliation"),
	DUPLICATE_AFFILIATION("html.report.duplicate_affiliation"),
	OPUS_ACCOUNT_BUT_NO_AD_ACCOUNT("html.report.opus_without_ad"),
	PERSONS_ON_LEAVE("html.report.persons_on_leave"),
	PERSONS_STOPPED("html.report.persons_stopped"),
	PERSONS_DISABLE_ACCOUNT_ORDERS("html.report.persons_disable_account_orders"),
	PERSONS_WITH_MULTIPLE_AFFILIATIONS("html.report.persons_multiple_affiliations"),
	PERSONS_WITH_SOFD_AFFILIATIONS("html.report.persons_with_sofd_affiliations"),
	PERSONS_WITH_ACTIVE_SOFD_AFFILIATIONS("html.report.persons_with_active_sofd_affiliations"),
	ACTIVE_AFFILIATION_OR_ACTIVE_AD_ACCOUNT("html.report.active_affiliation_or_active_ad"),
	PERSONS_WITH_AFFILIATIONS_WORKPLACES("html.report.persons_with_affiliations_workplaces");

	private String title;

	private ReportType(String title) {
		this.title = title;
	}
}