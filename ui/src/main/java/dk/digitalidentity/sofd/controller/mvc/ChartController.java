package dk.digitalidentity.sofd.controller.mvc;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.dto.ChartDTO;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Chart;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.ChartService;

@Controller
public class ChartController {
	
	@Autowired
	private SofdConfiguration config;

	@Autowired
	private ChartService chartService;
	
	@Autowired
	private OrgUnitService orgUnitService;

	@GetMapping("/chart/{uuid}")
	public String show(Model model, @PathVariable(name = "uuid") String uuid) {
		if (!config.getModules().getChart().isEnabled()) {
			model.addAttribute("error", "Diagram modul ikke slået til");
			return "simpleerror";
		}

		Chart chart = chartService.findByUuid(uuid);
		if (chart == null) {
			model.addAttribute("error", "Diagram ikke fundet");
			return "simpleerror";
		}
		
		// convert orgUnits to chart dtos. Every ChartDTO in the charts list is a diagram in the ui
		List<OrgUnit> allOus = orgUnitService.getAllActive();
		OrgUnit rootOU = allOus.stream().filter(o -> o.getParent() == null).findFirst().orElse(null);
		List<ChartDTO> charts = new ArrayList<>();
		List<String> uuidsInChart = new ArrayList<>();
		getChildrenRecursive(allOus, chart, rootOU, charts, false, null, 1, uuidsInChart);
		
		// maybe filter out managers that are inherited
		if (chart.isLeaderEnabled()) {
			for (ChartDTO dto : charts) {
				styleInheritedManagersRecursive(dto, null, uuidsInChart, chart.isHideInheritedManagers());
			}
		}
		
		// model
		model.addAttribute("charts", charts);
		model.addAttribute("showManager", chart.isLeaderEnabled());
		model.addAttribute("style", chart.getStyle());
		model.addAttribute("verticalStart", chart.getVerticalStart().getLevel());

		return "chart";
	}
	
	private void styleInheritedManagersRecursive(ChartDTO node, ChartDTO parent, List<String> uuids, boolean hideInheritedManagers) {
		if (node.isManagerInherited() && parent != null && uuids.contains(parent.getUuid())) {
			String managerName = hideInheritedManagers ? "&nbsp;" : node.getManager();
			node.setManager("<span class='inheritedManager'>" + managerName + "</span>");
		}
		
		for (ChartDTO child : node.getChildren()) {
			styleInheritedManagersRecursive(child, node, uuids, hideInheritedManagers);
		}
	}
	
	public void getChildrenRecursive(List<OrgUnit> allOus, Chart chart, OrgUnit currentNode, List<ChartDTO> charts, boolean inherit, ChartDTO currentChart, int currentLevel, List<String> uuidsInChart) {
		boolean shouldAdd = false;
		if (chart.getDepthLimit().isIncluded(currentLevel)) {
			if (inherit) {
				shouldAdd = true;
			} else {
				if (chart.getOrgUnits().stream().anyMatch(o -> o.getUuid().equals(currentNode.getUuid()))) {
					shouldAdd = true;
					inherit = chart.isInheritEnabled();
				}
			}
		}
		
		if (shouldAdd) {
			ChartDTO childChart = new ChartDTO();
			childChart.setName(currentNode.getName());
			childChart.setManager(currentNode.getManager() != null ? currentNode.getManager().getName() : "&nbsp;"); // whitespace needed to keep multiple charts aligned vertically
			childChart.setChildren(new ArrayList<>());
			childChart.setUuid(currentNode.getUuid());
			childChart.setManagerInherited(currentNode.getManager() != null ? currentNode.getManager().isInherited() : false);
			uuidsInChart.add(currentNode.getUuid());
			
			if (currentChart == null) {
				currentChart = childChart;
				charts.add(currentChart);
			} else {
				currentChart.getChildren().add(childChart);
				currentChart = childChart;
			}
		}
		for (OrgUnit childOu : allOus.stream().filter(o -> o.getParent() != null && o.getParent().getUuid().equalsIgnoreCase(currentNode.getUuid())).collect(Collectors.toList())) {
			getChildrenRecursive(allOus, chart, childOu, charts, inherit, currentChart, currentLevel+1, uuidsInChart);
		}
	}

}
