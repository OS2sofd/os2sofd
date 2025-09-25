package dk.digitalidentity.sofd.controller.rest.admin.model;

import java.util.List;
import java.util.stream.Collectors;

import dk.digitalidentity.sofd.dao.model.Chart;
import dk.digitalidentity.sofd.dao.model.enums.DepthLimit;
import dk.digitalidentity.sofd.dao.model.enums.VerticalStart;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OrganisationDiagramDTO {

	private long id;

	private String uuid;

	private String name;

	private boolean inheritEnabled;

	private boolean leaderEnabled;

	private DepthLimit depthLimit;	
	private int depthLimitValue;

	private VerticalStart verticalStart;
	private int verticalStartValue;

	private String style;

	private List<String> orgUnits;
	
	private boolean hideInheritedManagers;

	private List<Long> organisations;

	private Long organisation;

	public OrganisationDiagramDTO(Chart orgDiagram) {
		this.id = orgDiagram.getId();
		this.uuid = orgDiagram.getUuid();
		this.name = orgDiagram.getName();
		this.inheritEnabled = orgDiagram.isInheritEnabled();
		this.leaderEnabled = orgDiagram.isLeaderEnabled();
		this.depthLimit = orgDiagram.getDepthLimit();
		this.depthLimitValue = orgDiagram.getDepthLimit().getLevel();
		this.verticalStart = orgDiagram.getVerticalStart();
		this.verticalStartValue = orgDiagram.getVerticalStart().getLevel();
		this.style = orgDiagram.getStyle();
		this.orgUnits = orgDiagram.getOrgUnits().stream().map(ou -> ou.getUuid()).collect(Collectors.toList());
		this.hideInheritedManagers = orgDiagram.isHideInheritedManagers();
		this.organisation = orgDiagram.getOrganisation().getId();
	}

}
