package dk.digitalidentity.sofd.service.model;

import java.util.Set;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ADGridOrgUnit {
	private String uuid;
	private String name;
	private boolean inheritKle;
	private String parentUuid;
	private String managerUuid;
	private Set<String> klePrimary;
	private Set<String> kleSecondary;
	private Set<String> titles;
}
