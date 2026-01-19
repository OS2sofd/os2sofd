package dk.digitalidentity.sofd.dao.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "substitute_assignment")
@Getter
@Setter
@Audited
public class SubstituteAssignment {

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

	@BatchSize(size = 50)
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true, mappedBy = "substituteAssignment")
	private List<SubstituteAssignmentOrgUnitMapping> constraintMappings = new ArrayList<>();

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "person_uuid")
	private Person person;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "substitute_uuid")
	private Person substitute;
}
