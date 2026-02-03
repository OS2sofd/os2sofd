package dk.digitalidentity.sofd.config.properties;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Person {
	private boolean chosenNameEditable = false;
	private List<Long> chosenNameEditableForClientIds;
	private boolean resetChosenNameOnNameChange = false;
	private boolean resetChosenNameWhenInactive = false;
	private int resetChosenNameWhenInactiveDays = 14;
	//following two auto checks leave checkboxes as default value
	private boolean leaveAutoCheckIdM = false;
	private boolean leaveAutoCheckAD = false;
}
