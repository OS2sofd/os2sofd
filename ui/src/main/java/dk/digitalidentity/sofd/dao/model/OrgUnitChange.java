package dk.digitalidentity.sofd.dao.model;

import java.util.Date;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonBackReference;
import dk.digitalidentity.sofd.dao.model.enums.OrgUnitChangeChangeType;
import dk.digitalidentity.sofd.dao.model.enums.OrgUnitChangeStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "orgunit_changes")
public class OrgUnitChange {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@OneToOne
	@JoinColumn(name = "orgunit_uuid")
	@JsonBackReference
	private OrgUnit orgUnit;

	@Column
	private Date changedTimestamp;
	
	@Column
	private Date sentTimestamp;

	@Column
	@NotNull
	@Enumerated(EnumType.STRING)
	private OrgUnitChangeStatus status;
	
	@Column
	@NotNull
	@Enumerated(EnumType.STRING)
	private OrgUnitChangeChangeType changeType;
	
	@Column
	private String oldValue;
	
	@Column
	private String newValue;
}
