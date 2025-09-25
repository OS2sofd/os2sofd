package dk.digitalidentity.sofd.service.model.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum UsernameTemplateVariableType {
	STATIC("statisk"),
	FIRSTNAME("fornavn"),
	SURNAME("efternavn"),
	FULLNAME("fuldenavn"),
	CHOSENNAME("kaldenavn"),
	NAMESEQUENCE("navnesekvens"),
	LETTERS("bogstaver"),
	NUMBERS("tal"),
	SERIAL("løbenummer"),
	DATE("dato"),
	EMPLOYEEID("medarbejdernummer");
	
	private final String variableName;

	UsernameTemplateVariableType(String variableName) {
		this.variableName = variableName;
	}

	private static final Map<String, UsernameTemplateVariableType> LOOKUP_MAP;

	static {
		LOOKUP_MAP = Arrays.stream(values())
			.collect(Collectors.toMap(
					v -> v.variableName.toLowerCase(), // key: lowercased variable name
					Function.identity()                  					 // value: enum itself
			));
	}

	public static UsernameTemplateVariableType fromVariableName(String variableName) {
		var result = LOOKUP_MAP.get(variableName.trim().toLowerCase());
		if (result == null) {
			throw new IllegalArgumentException("Ugyldigt variabelnavn: " + variableName);
		}
		return result;
	}

}
