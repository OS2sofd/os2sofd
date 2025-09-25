package dk.digitalidentity.sofd.config.properties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Customer {
	private String cvr = "";
	private String notificationEmail = "";
}
