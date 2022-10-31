package dk.digitalidentity.sofd.config.properties;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OPUS {
	private boolean enableLosIdMatch = false;
	private boolean enableFiltering = false;
	private boolean enableAutoAffiliationConfiguration = false;
	private boolean enableActiveDirectoryEmployeeIdAssociation = false;
	private List<String> positionIds = new ArrayList<>();
	private List<String> losIds = new ArrayList<>();
	private String orgUnitInfix = "";
	
	// documentation only - not used here. Flag as false in docker-compose.yml
	// to indicate that the setting has been turned off
	private boolean filterEfterindtaegt = true;
}
