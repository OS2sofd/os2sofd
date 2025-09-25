package dk.digitalidentity.sofd.dao.model.enums;

import lombok.Getter;

@Getter
public enum UsernameType {
	AFFIXIAL("html.usertype.username.affixial"),
	TEMPLATE("html.usertype.username.template");
	
	private String title;
	
	private UsernameType(String title) {
		this.title = title;
	}
}
