package dk.digitalidentity.sofd.service.model;

import java.util.List;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ADGridPerson {
	private String personUuid;
	private String uuid;
	private String cpr;
	private String name;
	private String userId;
	private String email;
	private String phone;
	private String upn;
	private String nemloginUserUuid;
	private boolean prime;
	private boolean doNotInherit;
	private boolean disabled;
	private List<ADGridAffiliation> affiliations;
	private Set<String> klePrimary;
	private Set<String> kleSecondary;
	private boolean schoolUser;
}
