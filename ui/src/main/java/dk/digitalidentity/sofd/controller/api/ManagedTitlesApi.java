package dk.digitalidentity.sofd.controller.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import dk.digitalidentity.sofd.controller.api.dto.ManagedTitlesDTO;
import dk.digitalidentity.sofd.controller.api.dto.OrgUnitManagedTitlesDTO;
import dk.digitalidentity.sofd.dao.model.ManagedTitle;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.security.RequireDaoWriteAccess;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.service.OrgUnitService;

@RestController
public class ManagedTitlesApi {

	@Autowired
	private OrgUnitService orgUnitService;

	@RequireReadAccess
	@GetMapping("/api/managedTitles")
	public ResponseEntity<List<OrgUnitManagedTitlesDTO>> getManagedTitles() {
		List<OrgUnitManagedTitlesDTO> result = orgUnitService.getAll().stream()
				.filter(o -> !o.getManagedTitles().isEmpty())
				.map(o -> new OrgUnitManagedTitlesDTO(o))
				.collect(Collectors.toList());

		return new ResponseEntity<>(result, HttpStatus.OK);
	}

	@RequireDaoWriteAccess
	@PostMapping("/api/managedTitles/{ouUuid}")
	public ResponseEntity<String> postManagedTitles(@PathVariable String ouUuid, @RequestBody ManagedTitlesDTO dto) throws Exception {
		OrgUnit orgUnit = orgUnitService.getByUuid(ouUuid);
		if (orgUnit == null) {
			return new ResponseEntity<>("Enheden findes ikke: " + ouUuid, HttpStatus.NOT_FOUND);
		}

		List<String> managedTitlesFromOrgUnit = orgUnit.getManagedTitles()
				.stream()
				.map(m -> m.getName())
				.collect(Collectors.toList());

		boolean changes = false;

		// new titles
		for (String title : dto.getTitles()) {
			if (!managedTitlesFromOrgUnit.contains(title)) {
				ManagedTitle newTitle = new ManagedTitle();
				newTitle.setMaster(dto.getMaster());
				newTitle.setName(title);
				newTitle.setOrgUnit(orgUnit);
				
				orgUnit.getManagedTitles().add(newTitle);

				changes = true;
			}
		}

		// titles to be deleted
		List<ManagedTitle> toDelete = new ArrayList<>();
		for (ManagedTitle title : orgUnit.getManagedTitles()) {
			if (!Objects.equals(dto.getMaster(), title.getMaster())) {
				continue;
			}

			if (!dto.getTitles().contains(title.getName())) {
				toDelete.add(title);
				changes = true;
			}
		}

		if (!toDelete.isEmpty()) {
			orgUnit.getManagedTitles().removeAll(toDelete);
		}

		// save changes if any
		if (changes) {
			orgUnitService.save(orgUnit);
		}

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
