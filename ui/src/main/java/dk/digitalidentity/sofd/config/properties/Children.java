package dk.digitalidentity.sofd.config.properties;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class Children {
	// default set to 8 to support "omsorgsdage"
	private int maxAge = 8;
	private List<String> affiliationMasters = new ArrayList<>();
}
