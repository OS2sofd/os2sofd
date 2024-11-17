package dk.digitalidentity.sofd.dao.model.enums;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.Getter;

@Getter
public enum UsernameInfixType {
	RANDOM("html.usertype.username.infix.random"),
	SAME_AS_OTHER("html.usertype.username.infix.same_as_other"),
	FROM_NAME("html.usertype.username.infix.from_name"),
	FROM_NAME_SERIAL("html.usertype.username.infix.from_name_serial"),
	FROM_NAME_LONG("html.usertype.username.infix.from_name_long"),
	FROM_NAME_FULL("html.usertype.username.infix.from_name_full"),
	EMPLOYEE_ID("html.usertype.username.infix.employee_id"),
	NUMBER("html.usertype.username.infix.number"),
	NAME23SERIAL("html.usertype.username.infix.name23serial");

	private String message;

	private UsernameInfixType(String message) {
		this.message = message;
	}

	public static List<UsernameInfixType> filteredList(boolean includeSameAsOthers) {
		List<UsernameInfixType> result = new ArrayList<>();
		for (UsernameInfixType type : UsernameInfixType.values()) {
			if (includeSameAsOthers || !Objects.equals(type, SAME_AS_OTHER)) {
				result.add(type);
			}
		}
		
		return result;
	}
}
