package dk.digitalidentity.sofd.dao.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class SubstituteContext {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	@NotNull
	@Size(max = 255)
	private String name;

	@Column
	@NotNull
	@Size(max = 255)
	private String identifier;

	@Column
	private boolean supportsConstraints;

	@Column
	private boolean canBeDeleted = true;

	@Column
	private boolean assignableToOrgUnit;

	@Column
	private boolean inheritOrgUnitAssignments;
}
