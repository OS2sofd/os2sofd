package dk.digitalidentity.sofd.dao.model.enums;

import lombok.Getter;

@Getter
public enum SendTo {
	SEND_TO_MANAGER("html.entity.email_template.send_to.send_to_manager"),
	SEND_TO_MANAGER_OR_SUBSTITUTES("html.entity.email_template.send_to.send_to_manager_or_substitutes"),
	SEND_TO_MANAGER_AND_SUBSTITUTES("html.entity.email_template.send_to.send_to_manager_and_substitutes");
	
	private String title;

	private SendTo(String title) {
		this.title = title;
	}
}
