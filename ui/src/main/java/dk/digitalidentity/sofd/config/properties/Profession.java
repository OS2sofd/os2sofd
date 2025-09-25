package dk.digitalidentity.sofd.config.properties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Profession {

	// if enabled (default true) the mapping will be used everywhere,
	// if not enabled, the data is ONLY available in the API output,
	// and each integration must decide if they want to use it or not
	//
	// OBS! also add this to disable (views need it) : INSERT INTO settings (setting_key, setting_value) VALUES ('DISABLE_PROFESSIONS', 'true');
	//
	private boolean enableOutgoing = true;
	
	// by default we map against positionName when performing the mapping,
	// but we CAN change this to another supported field on request
	private ProfessionField field = ProfessionField.POSITION_NAME;
}
