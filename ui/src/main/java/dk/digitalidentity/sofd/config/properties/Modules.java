package dk.digitalidentity.sofd.config.properties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Modules {
	private AccountCreation accountCreation = new AccountCreation();
	private SmsGateway smsGateway = new SmsGateway();
	private PersonComments personComments = new PersonComments();
	private Los los = new Los();
	private Telephony telephony = new Telephony();
	private LocalLogin localLogin = new LocalLogin();
	private Profile profile = new Profile();
	private Badge badge = new Badge();
	private SubstituteConfiguration substitute = new SubstituteConfiguration();
	private HistoricalReportsOnOrgUnits historialReportsOnOrgunits = new HistoricalReportsOnOrgUnits();
	private FunctionHierarchy functionHierarchy = new FunctionHierarchy();
	private Manager manager = new Manager();
	private Chart chart = new Chart();
	private ContactPlaces contactPlaces = new ContactPlaces();
	private PositionDisplayName positionDisplayName = new PositionDisplayName();
	private Person person = new Person();
	private Affiliation affiliation = new Affiliation();
	private OrgUnitSubstitute orgUnitSubstitute = new OrgUnitSubstitute();
	private ManagerUI managerUI = new ManagerUI();
	private OrgUnit orgUnit = new OrgUnit();
	private Students students = new Students();
	private AffiliationWorkplace affiliationWorkplaces = new AffiliationWorkplace();
	private Profession professions = new Profession();
}
