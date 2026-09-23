package dk.digitalidentity.sofd.log;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;

import org.springframework.util.StringUtils;

// shared phrasing for audit log messages, so field changes read the same across controllers
public class AuditMessages {
	public static final String NOT_SET = "ikke angivet";

	public static String valueOrNotSet(String value) {
		return StringUtils.hasText(value) ? value : NOT_SET;
	}

	public static String dateOrNotSet(LocalDate date) {
		return (date != null) ? date.toString() : NOT_SET;
	}

	public static String dateOrNotSet(Date date) {
		return (date != null) ? new SimpleDateFormat("yyyy-MM-dd").format(date) : NOT_SET;
	}

	public static String describeChange(String field, String oldValue, String newValue) {
		return field + " ændret fra " + valueOrNotSet(oldValue) + " til " + valueOrNotSet(newValue);
	}
}
