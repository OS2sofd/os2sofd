package dk.digitalidentity.sofd.dao.model.enums;

import lombok.Getter;

@Getter
public enum UsernameSuffixType {
	NONE("html.usertype.username.suffix.none"),
	VALUE("html.usertype.username.suffix.value"),
	CREATE_DATE("html.usertype.username.suffix.create_date");
	
	private String message;

	private UsernameSuffixType(String message) {
		this.message = message;
	}
}
