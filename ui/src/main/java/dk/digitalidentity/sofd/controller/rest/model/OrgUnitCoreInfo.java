package dk.digitalidentity.sofd.controller.rest.model;

import javax.validation.constraints.Size;

import dk.digitalidentity.sofd.dao.model.OrgUnitTag;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrgUnitCoreInfo {

	@Size(min = 1, max = 64, message = "Skal være mellem 1 og 64 tegn")
	private String shortname;

	@Size(min = 1, max = 255, message = "Skal være mellem 1 og 255 tegn")
	private String sourceName;
	
	@Size(max = 255, message = "Må max være 255 tegn")
	private String displayName;

	private Long cvr;

	private Long senr;

	private Long pnr;
	
	private String street;
	
	private String postalCode;
	
	private String city;
	
	@Size(max = 255, message = "Maksimalt 255 tegn")
	private String costBearer;

	private String orgUnitType;

	@Size(min = 1, message = "Skal være udfyldt")
	private String parent;

	private String manager;
	
	// optional, only shown when feature is enabled, and will fail if existing OU exists with same masterId
	private String masterId;

	private Long belongsTo;

	private boolean doNotTransferToFKOrg;

	private boolean blockUpdate;

    private boolean inheritAddressFromParent;

	private List<OrgUnitTag> tags;
}
