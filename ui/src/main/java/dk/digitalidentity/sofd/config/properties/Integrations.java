package dk.digitalidentity.sofd.config.properties;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Integrations {
	private Kle kle = new Kle();
	private Email email = new Email();
	private EBoks eboks = new EBoks();
	private OS2sync os2sync = new OS2sync();
	private RoleCatalogue roleCatalogue = new RoleCatalogue();
	private OPUS opus = new OPUS();
	private AppManager appManager = new AppManager();

	@JsonIgnore
	private Cpr cpr = new Cpr();
	
	@JsonIgnore
	private Cvr cvr = new Cvr();

	@JsonIgnore
	private Children children = new Children();
}
