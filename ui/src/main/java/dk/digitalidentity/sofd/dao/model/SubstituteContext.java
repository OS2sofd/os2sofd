package dk.digitalidentity.sofd.dao.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
