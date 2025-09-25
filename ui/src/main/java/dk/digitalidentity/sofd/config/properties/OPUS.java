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
	private boolean adEmployeeIdAssociationLimitedToPrimeAffiliation = true;
	private List<String> positionIds = new ArrayList<>();
	private List<String> positionNames = List.of("efterindtægt");
	private List<String> losIds = new ArrayList<>();
	private String orgUnitInfix = "";
	private List<String> invalidPositionNames = new ArrayList<>();
}
