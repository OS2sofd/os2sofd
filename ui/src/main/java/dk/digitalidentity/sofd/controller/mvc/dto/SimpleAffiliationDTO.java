package dk.digitalidentity.sofd.controller.mvc.dto;

import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.service.AffiliationService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SimpleAffiliationDTO {
	private long id;
	private boolean prime;
	private String positionName;
	private String orgUnitName;
	private String employeeId;
	private String master;
	private boolean activeInFuture;
	private boolean inheritPrivileges;
	
	public SimpleAffiliationDTO(Affiliation affiliation) {
		this.id = affiliation.getId();
		this.prime = affiliation.isPrime();
		this.positionName = AffiliationService.getPositionName(affiliation);
		this.orgUnitName = affiliation.getCalculatedOrgUnit().getName();
		this.employeeId = affiliation.getEmployeeId();
		this.master = affiliation.getMaster();
		this.inheritPrivileges = affiliation.isInheritPrivileges();
	}
}
