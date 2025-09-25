package dk.digitalidentity.sofd.controller.mvc;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import dk.digitalidentity.sofd.controller.mvc.dto.KleDTO;
import dk.digitalidentity.sofd.controller.validation.AffiliationEditDTOValidator;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.service.AffiliationService;
import dk.digitalidentity.sofd.service.KleService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.model.KleAssignmentDto;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequireReadAccess
@Controller
public class AffiliationController {

	@Autowired
	private AffiliationService affiliationService;

	@Autowired
	private KleService kleService;
	
	@Autowired
	private AffiliationEditDTOValidator affiliationEditDTOValidator;

	@Autowired
	private OrgUnitService orgUnitService;

	@InitBinder("affiliationEditDTO")
	public void initAffiliationEditBinder(WebDataBinder binder) {
		binder.setValidator(affiliationEditDTOValidator);
	}

	@GetMapping("/ui/affiliation/view/{uuid}")
	public String view(Model model, @PathVariable("uuid") String uuid, @RequestParam(required = false, defaultValue = "orgUnit", value = "backRef") String backRef, @RequestParam(required = false, defaultValue = "false", value = "edit") boolean isEdit) throws Exception {
		Affiliation affiliation = affiliationService.findByUuid(uuid);
		List<OrgUnit> orgUnits = orgUnitService.getAll();
		if (affiliation == null) {
			log.warn("No Affiliation with uuid " + uuid);
			return "redirect:/";
		}

		if (AffiliationService.notActiveAnymore(affiliation)) {
			model.addAttribute("affiliationStopped", true);
		}
		else {
			List<KleDTO> kleDTOs = kleService.findAll().stream().map(k -> new KleDTO(k)).sorted(Comparator.comparing(KleDTO::getId)).collect(Collectors.toList());
			model.addAttribute("affiliationStopped", false);
			model.addAttribute("allKles", kleDTOs);
		}

		model.addAttribute("isEdit", isEdit);
		model.addAttribute("backRef", backRef);
		model.addAttribute("affiliation", affiliation);
		model.addAttribute("ous", orgUnitService.getAllTree());
		model.addAttribute("ousNoTree", orgUnits);

		return "affiliation/view";
	}

	@GetMapping("/ui/affil/viewKLE/{uuid}/{type}")
	public String getViewKLEFragment(Model model, @PathVariable("uuid") String uuid, @PathVariable("type") String type) {
		Affiliation affiliation = affiliationService.findByUuid(uuid);
		if (affiliation == null) {
			log.error("Invalid affiliation: " + uuid);			
			return "fragments/viewKLE :: content";
		}

		List<String> codes = new ArrayList<>();
		switch (type) {
			case "KlePrimary":
				codes = affiliation.getKlePrimary().stream().map(k -> k.getKleValue()).collect(Collectors.toList());
				break;
			case "KleSecondary":
				codes = affiliation.getKleSecondary().stream().map(k -> k.getKleValue()).collect(Collectors.toList());
				break;
			default:
				log.error("Invalid kle type: " + type);
				break;
		}

		List<KleAssignmentDto> kles = new ArrayList<>();
		if (codes != null) {
			for (String code : codes) {
				KleAssignmentDto kle = new KleAssignmentDto();
				kle.setCode(code);
				kle.setTitle(kleService.getName(code));
				
				kles.add(kle);
			}
		}

		model.addAttribute("kles", kles);

		return "fragments/viewKLE :: content";
	}

	@GetMapping("/ui/affil/editKLE/{uuid}/{type}")
	public String getEditKLEFragment(Model model, @PathVariable("uuid") String uuid, @PathVariable("type") String type) {
		Affiliation affiliation = affiliationService.findByUuid(uuid);
		if (affiliation == null) {
			log.error("Invalid affiliation: " + uuid);
			return "fragments/editKLE :: content";
		}

		List<String> kleSelected = new ArrayList<String>();
		switch (type) {
			case "KlePrimary":
				kleSelected = affiliation.getKlePrimary().stream().map(k -> k.getKleValue()).collect(Collectors.toList());
				break;
			case "KleSecondary":
				kleSelected = affiliation.getKleSecondary().stream().map(k -> k.getKleValue()).collect(Collectors.toList());
				break;
			default:
				log.error("Invalid kle type: " + type);
				break;
		}

		model.addAttribute("type", type);
		model.addAttribute("kleSelected", kleSelected);

		return "fragments/editKLE :: content";
	}

	@GetMapping("/ui/affil/core/{uuid}/{type}")
	public String getCoreFragment(Model model, @PathVariable("uuid") String uuid, @PathVariable("type") String type) {
		Affiliation affiliation = affiliationService.findByUuid(uuid);
		if (affiliation == null) {
			log.warn("No person with uuid: " + uuid);
			return "redirect:/ui/person";
		}

		model.addAttribute("affiliation", affiliation);

		if (type.equals("edit")) {
			return "affiliation/fragments/affiliation_core_edit :: affiliationCoreEdit";
		}

		return "affiliation/fragments/affiliation_core_view :: affiliationCoreView";
	}

	@GetMapping("/ui/affil/{uuid}/workplaces")
	public String getWorkplacesFragment(Model model, @PathVariable("uuid") String uuid) {
		Affiliation affiliation = affiliationService.findByUuid(uuid);
		if (affiliation == null) {
			log.error("Invalid affiliation: " + uuid);
			return "affiliation/fragments/workplaces :: workplaceList";
		}

		model.addAttribute("workplaces", affiliation.getWorkplaces());

		return "affiliation/fragments/workplaces :: workplaceList";
	}

}
