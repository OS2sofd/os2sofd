package dk.digitalidentity.sofd.config.properties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Person {
	private boolean chosenNameEditable = false;
	private boolean resetChosenNameOnNameChange = false;
	//following two auto checks leave checkboxes as default value
	private boolean leaveAutoCheckIdM = false;
	private boolean leaveAutoCheckAD = false;
}
