package dk.digitalidentity.sofd.config.properties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Person {
	private boolean chosenNameEditable = false;
	private boolean resetChosenNameOnNameChange = false;
}
