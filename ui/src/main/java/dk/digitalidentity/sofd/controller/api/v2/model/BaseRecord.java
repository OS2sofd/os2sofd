package dk.digitalidentity.sofd.controller.api.v2.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class BaseRecord {
	private static ObjectMapper mapper = new ObjectMapper();
	
	protected LocalDate toLocalDate(Date date) {
		if (date == null) {
			return null;
		}

		// might be an SQL instance, so convert to something that has a toInstant()
		// method on it
		if (date instanceof java.sql.Date) {
			date = new Date(date.getTime());
		}

		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
	}

	protected LocalDateTime toLocalDateTime(Date date) {
		return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
	}

	protected Date toDateEndOfDay(LocalDate localDate) {
		if (localDate == null) {
			return null;
		}

		return Date.from(localDate.atTime(23, 50, 0).atZone(ZoneId.systemDefault()).toInstant());
	}
	
	protected Date toDate(LocalDate localDate) {
		if (localDate == null) {
			return null;
		}

		return Date.from(localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
	}

	protected String mapToString(Map<String, Object> map) {
		if (map == null) {
			return null;
		}

		try {
			// sort before converting to String
			return mapper.writeValueAsString(new TreeMap<>(map));
		}
		catch (Exception ex) {
			log.error("Failed to convert map to string", ex);

			return null;
		}
	}

	@SuppressWarnings("unchecked")
	protected Map<String, Object> stringToMap(String localExtension) {
		if (!StringUtils.hasLength(localExtension)) {
			return null;
		}

		try {
			Map<String, Object> map = mapper.readValue(localExtension, Map.class);

			// return sorted
			return new TreeMap<>(map);
		}
		catch (Exception ex) {
			log.error("Failed to convert string to map", ex);

			return null;
		}
	}
}
