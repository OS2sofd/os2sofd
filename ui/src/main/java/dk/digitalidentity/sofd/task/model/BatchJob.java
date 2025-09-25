package dk.digitalidentity.sofd.task.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;
import java.util.function.Supplier;

import org.apache.commons.lang.time.DateUtils;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Builder
public class BatchJob {
	private String name;
	private LocalTime time;
	private Supplier<Boolean> function;
	@Setter
	private Date lastExecutionTime;

	@Setter
	private Date lastErrorTime;

	@Setter
	private long errorCount;
	
	@Builder.Default
	private DayOfWeek dayOfWeek = null;

	@Builder.Default
	private long maxExecutionAttempts = 3;
	
	public boolean shouldRun() {
		if(errorCount >= maxExecutionAttempts && lastErrorTime != null && DateUtils.isSameDay(lastErrorTime, new Date())) {
			return false;
		}

		if (lastExecutionTime != null && DateUtils.isSameDay(lastExecutionTime, new Date())) {
			return false;
		}
		
		if (dayOfWeek != null && !LocalDate.now().getDayOfWeek().equals(dayOfWeek)) {
			return false;
		}

		if (LocalTime.now().isAfter(time)) {
			return true;
		}

		return false;
	}

	public String toString() {
		return "  - " + padName(name, 40) + " : " + time.toString() + ((dayOfWeek != null) ? (" (" + dayOfWeek.toString() + ")") : "") +"\n";
	}
	
	private String padName(String name, int size) {
		if (name.length() > size) {
			return name.substring(0, size);
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append(name);
		while (builder.length() < size) {
			builder.append(" ");
		}

		return builder.toString();
	}
}
