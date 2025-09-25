package dk.digitalidentity.sofd.dao.model;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import org.hibernate.envers.Audited;

import lombok.Getter;
import lombok.Setter;

@Entity(name = "substitute_assignment_orgunit")
@Getter
@Setter
@Audited
public class SubstituteAssignmentOrgUnitMapping {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "substitute_assignment_id")
	@NotNull
	private SubstituteAssignment substituteAssignment;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "orgunit_uuid")
	@NotNull
	private OrgUnit orgUnit;
}
