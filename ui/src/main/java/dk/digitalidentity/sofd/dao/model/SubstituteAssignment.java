package dk.digitalidentity.sofd.dao.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

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
