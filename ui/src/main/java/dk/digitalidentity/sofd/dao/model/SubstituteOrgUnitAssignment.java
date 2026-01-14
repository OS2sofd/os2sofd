package dk.digitalidentity.sofd.dao.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import lombok.Getter;
import lombok.Setter;

@Entity(name = "substitute_org_unit_assignment")
@Getter
@Setter
@Audited
public class SubstituteOrgUnitAssignment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	
	@CreationTimestamp
	@Column
	private LocalDateTime created;
	
	@Column
	private LocalDateTime changed;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "substitute_context_id")
	@NotNull
	@NotAudited
	private SubstituteContext context;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "org_unit_uuid")
	private OrgUnit orgUnit;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "substitute_uuid")
	private Person substitute;
}
