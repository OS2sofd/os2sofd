package dk.digitalidentity.sofd.controller.mvc.dto.history;

import java.util.Date;

import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.service.AffiliationService;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HistoryAffiliation {
	private String master;
	private String masterId;
	private Date startDate;
	private Date stopDate;
	private boolean deleted;
	private boolean inheritPrivileges;
	private boolean prime;
	private String orgUnitName;
	private String orgUnitUuid;
	private String employeeId;
	private String employmentTerms;
	private String employmentTermsText;
	private Double workingHoursDenominator;
	private Double workingHoursNumerator;
	private String affiliationType;
	private String positionId;
	private String positionName;
	private String positionTypeId;
	private String positionTypeName;
	private String vendor;
	private String internalReference;
	
	public HistoryAffiliation(Affiliation affiliation) {
		this.master = affiliation.getMaster();
		this.masterId = affiliation.getMasterId();
		this.startDate = affiliation.getStartDate();
		this.stopDate = affiliation.getStopDate();
		this.deleted = affiliation.isDeleted();
		this.inheritPrivileges = affiliation.isInheritPrivileges();
		this.prime = affiliation.isPrime();
		if (affiliation.getOrgUnit() != null) {
			this.orgUnitName = affiliation.getOrgUnit().getName();
			this.orgUnitUuid = affiliation.getOrgUnit().getUuid();
		}
		this.employeeId = affiliation.getEmployeeId();
		this.employmentTerms = affiliation.getEmploymentTerms();
		this.employmentTermsText = affiliation.getEmploymentTermsText();
		this.workingHoursDenominator = affiliation.getWorkingHoursDenominator();
		this.workingHoursNumerator = affiliation.getWorkingHoursNumerator();
		this.affiliationType = (affiliation.getAffiliationType() != null) ? affiliation.getAffiliationType().toString() : null;
		this.positionId = affiliation.getPositionId();
		this.positionName = AffiliationService.getPositionName(affiliation);
		this.positionTypeId = affiliation.getPositionTypeId();
		this.positionTypeName = affiliation.getPositionName();
		this.vendor = affiliation.getVendor();
		this.internalReference = affiliation.getInternalReference();
	}
}
