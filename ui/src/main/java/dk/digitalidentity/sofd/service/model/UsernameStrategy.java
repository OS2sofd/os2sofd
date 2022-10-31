package dk.digitalidentity.sofd.service.model;

import lombok.Getter;

@Getter
public enum UsernameStrategy {
	RANDOM_USERNAME("html.setting.usernamestrategy.random"),
	FROM_NAME("html.setting.usernamestrategy.fromname"),
	NONE("html.setting.usernamestrategy.none");
	
	private String message;
	
	private UsernameStrategy(String message) {
		this.message = message;
	}
}
