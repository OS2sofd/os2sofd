package dk.digitalidentity.sofd.service.model;

import lombok.Getter;

@Getter
public enum RequestOpusType {
	SAME_AS_AD("html.setting.opus.strategy.same_as_ad"),
	RANDOM_USERNAME("html.setting.opus.strategy.random"),
	FROM_NAME("html.setting.opus.strategy.fromname");
	
	private String message;
	
	private RequestOpusType(String message) {
		this.message = message;
	}
}
