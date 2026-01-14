package dk.digitalidentity.sofd.dao.model;

import java.util.List;

import dk.digitalidentity.sofd.dao.model.enums.DepthLimit;
import dk.digitalidentity.sofd.dao.model.enums.VerticalStart;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Chart {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column
	private String uuid;

	@Column
	private String name;

	@Column
	private boolean inheritEnabled;

	@Column
	private boolean leaderEnabled;

	@Column
	@Enumerated(EnumType.STRING)
	private DepthLimit depthLimit;

	@Column
	@Enumerated(EnumType.STRING)
	private VerticalStart verticalStart;

	@Column
	private String style;
	
	@Column
	private boolean hideInheritedManagers;

	@OneToMany
	@JoinTable(name = "chart_orgunit", joinColumns = @JoinColumn(name = "chart_id"), inverseJoinColumns = @JoinColumn(name = "orgunit_uuid"))
	private List<OrgUnit> orgUnits;

	@ManyToOne
	@JoinColumn(name = "organisation_id", nullable = false)
	private Organisation organisation;
}
