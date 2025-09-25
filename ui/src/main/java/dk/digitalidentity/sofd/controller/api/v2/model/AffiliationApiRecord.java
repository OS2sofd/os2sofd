package dk.digitalidentity.sofd.controller.api.v2.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonFormat;

import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.dao.model.enums.AccountOrderDeactivateAndDeleteRule;
import dk.digitalidentity.sofd.dao.model.enums.AffiliationType;
import dk.digitalidentity.sofd.dao.model.mapping.AffiliationFunctionMapping;
import dk.digitalidentity.sofd.service.AffiliationService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@ToString(exclude = { "localExtensions" })
@NoArgsConstructor
public class AffiliationApiRecord extends BaseRecord {
	// primary key
	
	@NotNull
	private String master;

	@NotNull
	private String masterId;

	// read/write fields
	
	@Pattern(regexp = "([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})", message = "Invalid uuid")
	private String uuid;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDate startDate;
	
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	private LocalDate stopDate;
	
	private String employeeId;
	private String employmentTerms;
	private String employmentTermsText;
	private Map<String, Object> localExtensions;
	private String payGrade;
	private String payGradeText;
	private String wageStep;
	private Double workingHoursDenominator;
	private Double workingHoursNumerator;
	private String affiliationType;
	private String positionId;
	private String positionName;
	private String positionShort;
	private String positionTypeId;
	private String positionTypeName;
	private String superiorLevel;
	private String subordinateLevel;
	private Set<String> functions;

	// TODO: remove at some point once we no longer manage deleted from our AD integration (which we really should stop doing)
	private Boolean deleted;

	@Pattern(regexp = "([a-f0-9]{8}(-[a-f0-9]{4}){4}[a-f0-9]{8})", message = "Invalid uuid")
	private String orgUnitUuid;
	private String alternativeOrgUnitUuid;

	// read-only
	private boolean prime;
	private Boolean inheritPrivileges;
	private String personUuid;
	private String positionDisplayName;
	private String professionName;
	private String vendor;
	private AccountOrderDeactivateAndDeleteRule deactivateAndDeleteRule;
	
	public AffiliationApiRecord(Affiliation affiliation) {
		this.master = affiliation.getMaster();
		this.masterId = affiliation.getMasterId();
		this.personUuid = affiliation.getPerson().getUuid();
		this.uuid = affiliation.getUuid();
		this.startDate = toLocalDate(affiliation.getStartDate());
		this.stopDate = toLocalDate(affiliation.getStopDate());
		this.inheritPrivileges = affiliation.isInheritPrivileges();
		this.deleted = affiliation.isDeleted();
		this.employeeId = affiliation.getEmployeeId();
		this.employmentTerms = affiliation.getEmploymentTerms();
		this.employmentTermsText = affiliation.getEmploymentTermsText();
		this.payGrade = affiliation.getPayGrade();
		this.payGradeText = affiliation.getPayGradeText();
		this.wageStep = affiliation.getWageStep();
		this.workingHoursDenominator = affiliation.getWorkingHoursDenominator();
		this.workingHoursNumerator = affiliation.getWorkingHoursNumerator();
		this.affiliationType = (affiliation.getAffiliationType() != null) ? affiliation.getAffiliationType().toString() : null;
		this.positionId = affiliation.getPositionId();
		this.positionName = affiliation.getPositionName();
		this.positionShort = affiliation.getPositionShort();
		this.positionTypeId = affiliation.getPositionTypeId();
		this.positionTypeName = affiliation.getPositionTypeName();
		this.superiorLevel = affiliation.getSuperiorLevel();
		this.subordinateLevel = affiliation.getSubordinateLevel();
		this.localExtensions = stringToMap(affiliation.getLocalExtensions());
		this.prime = affiliation.isPrime();
		this.positionDisplayName = AffiliationService.getPositionName(affiliation);
		this.vendor = affiliation.getVendor();

		if (affiliation.getFunctions() != null) {
			this.functions = new HashSet<String>();
			
			for (AffiliationFunctionMapping affiliationFunction : affiliation.getFunctions()) {
				this.functions.add(affiliationFunction.getFunction().toString());
			}
		}

		if (affiliation.getOrgUnit() != null) {
			this.orgUnitUuid = affiliation.getOrgUnit().getUuid();
		}
		
		if (affiliation.getProfession() != null) {
			this.professionName = affiliation.getProfession().getName();
		}

		// pso: here we use the calculated orgunit method so we cover both the alternative orgunit case and the workplaces case
		// consider refactoring these cases to 2 separate fields in the api or rename it to calculated orgunit, but then onprem agents would need to be updated as well.
		this.alternativeOrgUnitUuid = affiliation.getCalculatedOrgUnit().getUuid();
		this.deactivateAndDeleteRule = affiliation.getDeactivateAndDeleteRule();
	}

	public Affiliation toAffiliation(Person person) {		
		Affiliation affiliation = new Affiliation();
		
		// the supplied person might have the same affiliation, in which case all back-references should go to that instance (make Hibernate happy)
		Affiliation actualAffiliation = affiliation;
		if (person.getAffiliations() != null && person.getAffiliations().size() > 0) {
			for (Affiliation pAff : person.getAffiliations()) {
				if (Objects.equals(pAff.getMaster(), this.master) && Objects.equals(pAff.getMasterId(), this.masterId)) {
					actualAffiliation = pAff;
					break;
				}
			}
		}
		
		affiliation.setAffiliationType((affiliationType != null) ? AffiliationType.valueOf(affiliationType) : AffiliationType.EMPLOYEE);
		affiliation.setEmployeeId(employeeId);
		affiliation.setEmploymentTerms(employmentTerms);
		affiliation.setEmploymentTermsText(employmentTermsText);
		affiliation.setDeleted(deleted != null ? deleted : false);
		affiliation.setLocalExtensions(mapToString(localExtensions));
		affiliation.setMaster(master);
		affiliation.setMasterId(masterId);
		affiliation.setOrgUnit(OrgUnitService.getInstance().getByUuid(orgUnitUuid));
//		affiliation.setAlternativeOrgUnit(OrgUnitService.getInstance().getByUuid(alternativeOrgUnitUuid)); // TODO this would make the field read and write
		affiliation.setPayGrade(payGrade);
		affiliation.setPayGradeText(payGradeText);
		affiliation.setWageStep(wageStep);
		affiliation.setPerson(person);
		affiliation.setPositionId(positionId);
		affiliation.setPositionName(positionName);
		affiliation.setPositionShort(positionShort);
		affiliation.setPositionTypeId(positionTypeId);
		affiliation.setPositionTypeName(positionTypeName);
		affiliation.setSuperiorLevel(superiorLevel);
		affiliation.setSubordinateLevel(subordinateLevel);
		affiliation.setStartDate((startDate != null) ? toDate(startDate) : null);
		affiliation.setStopDate((stopDate != null) ? toDateEndOfDay(stopDate) : null);
		affiliation.setUuid(uuid);
		affiliation.setWorkingHoursDenominator(workingHoursDenominator);
		affiliation.setWorkingHoursNumerator(workingHoursNumerator);

		// sanity check for start/stop-dates
		if (affiliation.getStopDate() != null && affiliation.getStartDate() != null && affiliation.getStopDate().before(affiliation.getStartDate())) {
			log.warn("Got affiliation with stop-date BEFORE start-date on: " + person.getUuid() + " - setting stopDate to StartDate");

			affiliation.setStopDate(affiliation.getStartDate());
		}

		if (functions != null) {
			affiliation.setFunctions(new ArrayList<>());

			for (String function : functions) {
				AffiliationFunctionMapping fMap = new AffiliationFunctionMapping();
				fMap.setAffiliation(actualAffiliation);
				fMap.setFunction(function);

				affiliation.getFunctions().add(fMap);
			}
		}
		
		return affiliation;
	}
}
