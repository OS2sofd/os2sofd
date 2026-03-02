package dk.digitalidentity.sofd.dao.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.apache.commons.lang.StringUtils;
import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.data.annotation.ReadOnlyProperty;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import dk.digitalidentity.sofd.dao.model.enums.AccountOrderDeactivateAndDeleteRule;
import dk.digitalidentity.sofd.dao.model.enums.AffiliationType;
import dk.digitalidentity.sofd.dao.model.mapping.AffiliationFunctionMapping;
import dk.digitalidentity.sofd.dao.model.mapping.AffiliationPrimaryKleMapping;
import dk.digitalidentity.sofd.dao.model.mapping.AffiliationSecondaryKleMapping;
import dk.digitalidentity.sofd.serializer.LocalExtensionsDeserializer;
import dk.digitalidentity.sofd.serializer.LocalExtensionsSerializer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Audited
@Entity(name = "affiliations")
@Getter
@Setter
@EqualsAndHashCode(exclude = { "person", "orgUnit", "alternativeOrgUnit" }, callSuper = true)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Affiliation extends MasteredEntity implements Serializable {
	private static final long serialVersionUID = -1443788795789576943L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	@NotNull
	private String uuid;

	@Column
	@NotNull
	private String master;

	@Column
	@NotNull
	private String masterId;

	@Column
	private Date startDate;

	@Column
	private Date stopDate;

	// TODO: this should be removed from the datamodel at some point (need to update agents to support this change)
	@Column
	private boolean deleted;

	// used for internal rules with regards to account creation
	@JsonIgnore
	@Column
	private boolean inheritPrivileges = true; // defaults to true when constructed

	// maintained by batch job and write intercepter
	@Column
	@ReadOnlyProperty
	private boolean prime;

	// only maintained by gui - used to override automatic prime logic
	@Column
	@JsonIgnore
	@ReadOnlyProperty
	private boolean selectedPrime;

	@BatchSize(size = 50)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "orgunit_uuid")
	@NotNull
	private OrgUnit orgUnit;

	@BatchSize(size = 50)
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "alt_orgunit_uuid")
	private OrgUnit alternativeOrgUnit;

	@BatchSize(size = 50)
	@ManyToOne(fetch = FetchType.LAZY)
	@JsonBackReference
	private Person person;

	@BatchSize(size = 50)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "profession_id")
	@NotAudited
	private Profession profession;

	@Column
	@Size(max = 255)
	private String employeeId;

	@Column
	@Size(max = 255)
	private String employmentTerms;

	@Column
	@Size(max = 255)
	private String employmentTermsText;

	@Column
	@Size(max = 255)
	private String payGrade;

	@Column
	@Size(max = 255)
	private String payGradeText;

	@Column
	@Size(max = 255)
	private String wageStep;

	@Column
	private Double workingHoursDenominator;

	@Column
	private Double workingHoursNumerator;

	@Column
	@NotNull
	@Enumerated(EnumType.STRING)
	private AffiliationType affiliationType;

	@Column
	@Size(max = 255)
	private String positionId;

	@Column
	@NotNull
	@Size(max = 255)
	private String positionName;

	@Column
	@NotNull
	@Size(max = 255)
	private String positionShort;

	@Column
	@Size(max = 255)
	private String positionTypeId;

	@Column
	@Size(max = 255)
	private String positionTypeName;

	@Column
	@Size(max = 255)
	private String vendor;

	@Column
	@Size(max = 255)
	private String internalReference;

	@Column
	@Size(max = 255)
	private String positionDisplayName;
	
	@Column
	private String superiorLevel;
	
	@Column
	private String subordinateLevel;
	
	@Column
	private boolean doNotTransferToFkOrg;

	// this can be sat to true when creating an affiliation in the future. When the affiliation becomes active it will be primary and this field will be false
	@Column
	private boolean useAsPrimaryWhenActive;

	@BatchSize(size = 100)
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "affiliation")
	private List<AffiliationPrimaryKleMapping> klePrimary;

	@BatchSize(size = 100)
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "affiliation")
	private List<AffiliationSecondaryKleMapping> kleSecondary;

	@BatchSize(size = 50)
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "affiliation")
	private List<AffiliationFunctionMapping> functions;

	@Column
	@JsonSerialize(using = LocalExtensionsSerializer.class)
	@JsonDeserialize(using = LocalExtensionsDeserializer.class)
	private String localExtensions;

	@BatchSize(size = 50)
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "affiliation")
	private List<Workplace> workplaces;

	@Column
	@NotNull
	@Enumerated(EnumType.STRING)
	private AccountOrderDeactivateAndDeleteRule deactivateAndDeleteRule;

	// transient field used by data-control-flow in our EntityListeners
	@JsonIgnore
	private transient boolean transientFlagNewAffiliation = false;

	public void setPositionDisplayName(String positionDisplayName) {
		this.positionDisplayName = StringUtils.isBlank(positionDisplayName) ? null : positionDisplayName;
	}

	public OrgUnit getCalculatedOrgUnit() {
		OrgUnit calculatedOrgUnit = this.alternativeOrgUnit != null ? alternativeOrgUnit : orgUnit;
		
		if (this.workplaces != null && !this.workplaces.isEmpty()) {
			LocalDate today = LocalDate.now();
			Workplace currentWorkplace = this.getWorkplaces().stream().filter(w -> (w.getStartDate().isBefore(today) || w.getStartDate().equals(today)) && (w.getStopDate().isAfter(today) || w.getStopDate().equals(today))).findFirst().orElse(null);

			if (currentWorkplace != null) {
				calculatedOrgUnit = currentWorkplace.getOrgUnit();
			}
		}
		return calculatedOrgUnit;
	}

	public boolean isFromWageSystem() {
		return this.master.equalsIgnoreCase("OPUS") || this.master.startsWith("SD-");
	}
}