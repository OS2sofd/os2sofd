package dk.digitalidentity.sofd.service.model;

import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ADGridAffiliation {
	private String affiliationUuid;
	private String positionName;
	private String professionName;
	private String rawPositionName;
	private String orgUnitUuid;
	private boolean doNotInherit;
	private Map<String, Object> localExtensions;
	private Set<String> functions;
}
