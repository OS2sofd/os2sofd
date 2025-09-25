package dk.digitalidentity.sofd.service.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ADGridAD {
	private String personUuid;
	private String uuid;
	private String cpr;
	private String name;
	private String userId;
	private boolean disabled;
	private boolean expired;
	private boolean prime;
	private String email;
	private String upn;
	private String primaryOrgunitName;
	private String passwordExpireDate;
	private String localExtensions;
	private boolean robot;
}
