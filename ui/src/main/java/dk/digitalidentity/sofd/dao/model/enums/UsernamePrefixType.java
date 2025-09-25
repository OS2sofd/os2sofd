package dk.digitalidentity.sofd.dao.model.enums;

import lombok.Getter;

@Getter
public enum UsernamePrefixType {
	NONE("html.usertype.username.prefix.none"),
	VALUE("html.usertype.username.prefix.value"),
	LETTER2NUMBERS("html.usertype.username.prefix.letter2numbers"),
	CREATE_DATE("html.usertype.username.prefix.create_date");
	
	private String message;

	private UsernamePrefixType(String message) {
		this.message = message;
	}
}
