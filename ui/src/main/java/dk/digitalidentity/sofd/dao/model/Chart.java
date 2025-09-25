package dk.digitalidentity.sofd.dao.model;

import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import dk.digitalidentity.sofd.dao.model.enums.DepthLimit;
import dk.digitalidentity.sofd.dao.model.enums.VerticalStart;
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
