package dk.digitalidentity.sofd.controller.rest.admin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.controller.rest.admin.model.OrganisationDiagramDTO;
import dk.digitalidentity.sofd.dao.model.Chart;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.Organisation;
import dk.digitalidentity.sofd.dao.model.enums.DepthLimit;
import dk.digitalidentity.sofd.dao.model.enums.VerticalStart;
import dk.digitalidentity.sofd.security.RequireAdminAccess;
import dk.digitalidentity.sofd.service.ChartService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.OrganisationService;
import dk.digitalidentity.sofd.service.model.OUTreeForm;

@RequireAdminAccess
@RestController
public class OrganisationsdiagramRestController {

	@Autowired
	private ChartService chartService;
	
	@Autowired
	private OrgUnitService orgUnitService;

	@Autowired
    private OrganisationService organisationService;

	@GetMapping("/rest/admin/chart/{id}")
	public ResponseEntity<?> getOrganisationDiagram(@PathVariable("id") long id) {
		Chart orgDiagram = chartService.findById(id);
		if (orgDiagram == null) {
			return ResponseEntity.notFound().build();
		}

		return ResponseEntity.ok(new OrganisationDiagramDTO(orgDiagram));
	}

	@PostMapping("/rest/admin/chart/update")
	public ResponseEntity<?> updateOrganisationDiagram(@RequestBody OrganisationDiagramDTO body) {
		Chart orgDiagram = chartService.findById(body.getId());
		if (orgDiagram == null) {
			//create
			orgDiagram = new Chart();
			orgDiagram.setUuid(UUID.randomUUID().toString());
			orgDiagram.setOrgUnits(new ArrayList<>());
		}

		//update
		orgDiagram.setDepthLimit(DepthLimit.findByKey(body.getDepthLimitValue()));
		orgDiagram.setVerticalStart(VerticalStart.findByKey(body.getVerticalStartValue()));
		orgDiagram.setInheritEnabled(body.isInheritEnabled());
		orgDiagram.setLeaderEnabled(body.isLeaderEnabled());
		orgDiagram.setHideInheritedManagers(body.isHideInheritedManagers());
		orgDiagram.setName(body.getName());
		orgDiagram.setStyle(body.getStyle());
		orgDiagram.setOrganisation(organisationService.getById(body.getOrganisation()));
		if (body.getOrgUnits() != null) {
			for (String ou : body.getOrgUnits()) {
				OrgUnit orgUnit = orgUnitService.getByUuid(ou);
				if (orgUnit != null) {
					if (!orgDiagram.getOrgUnits().contains(orgUnit)) {
						orgDiagram.getOrgUnits().add(orgUnit);
					}
				}
			}
		}

		orgDiagram.getOrgUnits().removeIf(ou -> !body.getOrgUnits().contains(ou.getUuid()));
		chartService.save(orgDiagram);
		
		return ResponseEntity.ok().build();
	}

	@DeleteMapping("/rest/admin/chart/delete/{id}")
	public ResponseEntity<?> deleteOrganisationDiagram(@PathVariable("id") long id) {
		Chart orgDiagram = chartService.findById(id);
		if (orgDiagram == null) {
			return ResponseEntity.notFound().build();
		}

		chartService.delete(orgDiagram);

		return ResponseEntity.ok().build();
	}

	@GetMapping("/rest/admin/chart/orgUnitTree/{limit}/{id}")
	public ResponseEntity<?> getOrganisationDiagram(@PathVariable("limit") int limit, @PathVariable("id") long id) {
		List<OUTreeForm> allOrgUnits;
		Organisation organisation = id == 0 ? organisationService.getAdmOrg() : organisationService.getById(id);


		if (organisation == null) {
			return ResponseEntity.badRequest().build();
		}
		else {
			allOrgUnits = new ArrayList<>(orgUnitService.getAllTree(organisation));
		}

		if (limit == 0) {
			return ResponseEntity.ok(allOrgUnits);
		}

		List<OUTreeForm> toBeDeleted = new ArrayList<>();

		for (Iterator<OUTreeForm> iterator = allOrgUnits.iterator(); iterator.hasNext();) {
			OUTreeForm entry = iterator.next();
			if (toBeDeleted.contains(entry)) {
				iterator.remove();
				continue;
			}

			try {
				int parents = countParents(entry, allOrgUnits);
				if (parents > limit) {
					removeAllChildren(entry, allOrgUnits, toBeDeleted);
					iterator.remove();
				}
			} catch (Exception e) {
				//Broken parents chain so we remove
				removeAllChildren(entry, allOrgUnits, toBeDeleted);
				iterator.remove();
			}
			
		}

		return ResponseEntity.ok(allOrgUnits);
	}

	private int countParents(OUTreeForm child, List<OUTreeForm> allOrgUnits) throws Exception {
		if (child.getParent().equals("#")) {
			return 0;
		}

		OUTreeForm parent = allOrgUnits.stream().filter(e -> e.getId().equals(child.getParent())).findAny().orElseThrow(() -> new Exception(""));

		return 1 + countParents(parent, allOrgUnits);
	}

	private void removeAllChildren(OUTreeForm parent, List<OUTreeForm> allOrgUnits, List<OUTreeForm> toBeDeleted) {
		List<OUTreeForm> children = allOrgUnits.stream().filter(e -> e.getParent().equals(parent.getId())).collect(Collectors.toList());
		
		if (children.isEmpty()) {
			toBeDeleted.add(parent);
			return;
		}
		
		for (OUTreeForm child : children) {
			removeAllChildren(child, allOrgUnits, toBeDeleted);
		}

	}

}
