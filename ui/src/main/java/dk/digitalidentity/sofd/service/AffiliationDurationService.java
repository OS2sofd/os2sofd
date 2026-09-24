package dk.digitalidentity.sofd.service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import dk.digitalidentity.sofd.dao.OrgUnitDao;
import dk.digitalidentity.sofd.dao.OrgUnitDao.EffectiveAffiliationDurationRule;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.enums.AffiliationDurationRule;

/**
 * Enforces the maximum duration of manually created (master SOFD) affiliations, as configured
 * on the orgunit (or inherited from the nearest ancestor). Only enforced from the GUI, the APIs
 * are deliberately left alone so integrations keep working unchanged.
 */
@Service
public class AffiliationDurationService {

	public record EffectiveRule(AffiliationDurationRule rule, Integer maxDays, String sourceOrgUnitUuid, String sourceOrgUnitName) {

		public boolean isStopDateRequired() {
			return rule == AffiliationDurationRule.STOP_DATE_REQUIRED || rule == AffiliationDurationRule.MAX_DAYS;
		}

		public boolean isInherited(OrgUnit orgUnit) {
			return sourceOrgUnitUuid != null && !sourceOrgUnitUuid.equals(orgUnit.getUuid());
		}
	}

	@Autowired
	private OrgUnitDao orgUnitDao;

	public EffectiveRule resolve(OrgUnit orgUnit) {
		if (orgUnit == null) {
			return new EffectiveRule(AffiliationDurationRule.NO_LIMIT, null, null, null);
		}

		EffectiveAffiliationDurationRule row = orgUnitDao.getEffectiveAffiliationDurationRule(orgUnit.getUuid());
		if (row == null) {
			// INHERIT all the way to the root means no limit
			return new EffectiveRule(AffiliationDurationRule.NO_LIMIT, null, null, null);
		}

		AffiliationDurationRule rule = AffiliationDurationRule.valueOf(row.getRule());
		if (rule == AffiliationDurationRule.MAX_DAYS && (row.getMaxDays() == null || row.getMaxDays() <= 0)) {
			// misconfigured, treat as stop date required so we fail safe without blocking everything
			rule = AffiliationDurationRule.STOP_DATE_REQUIRED;
		}

		return new EffectiveRule(rule, row.getMaxDays(), row.getUuid(), row.getName());
	}

	/**
	 * Latest stop date allowed for an affiliation in the given orgunit with the given start date,
	 * or null if there is no maximum duration.
	 */
	public LocalDate latestStopDate(EffectiveRule effective, LocalDate startDate) {
		if (effective.rule() != AffiliationDurationRule.MAX_DAYS) {
			return null;
		}

		// an affiliation can always be extended to today plus the limit, so the window is measured from the later of start date and today
		LocalDate today = LocalDate.now();
		LocalDate from = (startDate != null && startDate.isAfter(today)) ? startDate : today;

		return from.plusDays(effective.maxDays());
	}

	/**
	 * Returns a user facing (Danish) error message if the stop date is not allowed, otherwise null.
	 */
	public String validate(OrgUnit orgUnit, LocalDate startDate, LocalDate stopDate) {
		EffectiveRule effective = resolve(orgUnit);

		if (stopDate == null) {
			if (effective.isStopDateRequired()) {
				return "Der skal angives en slutdato på tilhørsforhold i enheden " + orgUnit.getName() + describeSource(effective, orgUnit);
			}

			return null;
		}

		LocalDate latest = latestStopDate(effective, startDate);
		if (latest != null && stopDate.isAfter(latest)) {
			return "Slutdatoen må senest være " + latest + ", da tilhørsforhold i enheden " + orgUnit.getName() + " højst må have en varighed på " + effective.maxDays() + " dage" + describeSource(effective, orgUnit);
		}

		return null;
	}

	public String validate(OrgUnit orgUnit, Date startDate, Date stopDate) {
		return validate(orgUnit, toLocalDate(startDate), toLocalDate(stopDate));
	}

	/**
	 * Variant for the MVC forms, where dates arrive as yyyy-MM-dd strings. Unparseable values are
	 * ignored here, the format itself is validated elsewhere.
	 */
	public String validate(OrgUnit orgUnit, String startDate, String stopDate) {
		return validate(orgUnit, parse(startDate), parse(stopDate));
	}

	public String validate(Affiliation affiliation) {
		if (!"SOFD".equals(affiliation.getMaster())) {
			return null;
		}

		return validate(affiliation.getOrgUnit(), affiliation.getStartDate(), affiliation.getStopDate());
	}

	private static String describeSource(EffectiveRule effective, OrgUnit orgUnit) {
		if (effective.isInherited(orgUnit)) {
			return " (regel nedarvet fra " + effective.sourceOrgUnitName() + ")";
		}

		return "";
	}

	private static LocalDate parse(String value) {
		if (!StringUtils.hasLength(value)) {
			return null;
		}

		try {
			return toLocalDate(new SimpleDateFormat("yyyy-MM-dd").parse(value));
		}
		catch (ParseException ex) {
			return null;
		}
	}

	// java.sql.Date does not support toInstant(), so go through epoch millis
	private static LocalDate toLocalDate(Date date) {
		if (date == null) {
			return null;
		}

		return Instant.ofEpochMilli(date.getTime()).atZone(ZoneId.systemDefault()).toLocalDate();
	}
}
