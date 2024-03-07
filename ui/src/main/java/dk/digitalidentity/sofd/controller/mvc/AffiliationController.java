package dk.digitalidentity.sofd.controller.mvc;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.dto.AffiliationEditDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.KleDTO;
import dk.digitalidentity.sofd.controller.validation.AffiliationEditDTOValidator;
import dk.digitalidentity.sofd.dao.model.Affiliation;
import dk.digitalidentity.sofd.dao.model.Person;
import dk.digitalidentity.sofd.security.RequirePersonCreaterOrControllerWriteAccess;
import dk.digitalidentity.sofd.security.RequireReadAccess;
import dk.digitalidentity.sofd.security.SecurityUtil;
import dk.digitalidentity.sofd.service.AffiliationService;
import dk.digitalidentity.sofd.service.KleService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import dk.digitalidentity.sofd.service.PersonService;
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
	private PersonService personService;

	@Autowired
	private AffiliationEditDTOValidator affiliationEditDTOValidator;

	@Autowired
	private SofdConfiguration sofdConfiguration;

	@Autowired
	private OrgUnitService orgUnitService;

	@InitBinder("affiliationEditDTO")
	public void initAffiliationEditBinder(WebDataBinder binder) {
		binder.setValidator(affiliationEditDTOValidator);
	}

	@GetMapping("/ui/affiliation/view/{uuid}")
	public String view(Model model, @PathVariable("uuid") String uuid, @RequestParam(required = false, defaultValue = "orgUnit", value = "backRef") String backRef) throws Exception {
		Affiliation affiliation = affiliationService.findByUuid(uuid);
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

		model.addAttribute("backRef", backRef);
		model.addAttribute("affiliation", affiliation);
		model.addAttribute("ous", orgUnitService.getAllTree());

		return "affiliation/view";
	}
	
	@RequirePersonCreaterOrControllerWriteAccess
	@GetMapping("/ui/affiliation/edit/{uuid}")
	public String edit(Model model, @PathVariable("uuid") String uuid) {
		Affiliation affiliation = affiliationService.findByUuid(uuid);
		if (affiliation == null) {
			log.warn("No Affiliation with uuid " + uuid);
			return "redirect:/";
		}
		
		Set<String> constraintOUs = SecurityUtil.getOrgUnitUuidsFromConstraint();
		if (constraintOUs.size() > 0 && (!constraintOUs.contains(affiliation.getOrgUnit().getUuid()) || (affiliation.getAlternativeOrgUnit() != null && !constraintOUs.contains(affiliation.getAlternativeOrgUnit().getUuid())))) { // TODO: OK
			log.warn("Affiliation with uuid " + uuid + " can not be edited by this user");
			return "redirect:/";
		}

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		
		AffiliationEditDTO affiliationDTO = new AffiliationEditDTO();
		affiliationDTO.setUuid(affiliation.getUuid());
		affiliationDTO.setPositionName(affiliation.getPositionName());
		affiliationDTO.setStartDate((affiliation.getStartDate() != null) ? formatter.format(affiliation.getStartDate()) : "");
		affiliationDTO.setStopDate((affiliation.getStopDate() != null) ? formatter.format(affiliation.getStopDate()) : "");
		affiliationDTO.setPersonUuid(affiliation.getPerson().getUuid());
		affiliationDTO.setVendor(affiliation.getVendor());
		affiliationDTO.setInternalReference(affiliation.getInternalReference());
		affiliationDTO.setAffiliationType(affiliation.getAffiliationType());
		affiliationDTO.setPositionDisplayName(affiliation.getPositionDisplayName());
		affiliationDTO.setDoNotTransferToFKOrg(affiliation.isDoNotTransferToFkOrg());
		
		model.addAttribute("affiliationEditDTO", affiliationDTO);
		model.addAttribute("fromSofd", affiliation.getMaster().equals("SOFD"));

		return "affiliation/edit";
	}
	
	@RequirePersonCreaterOrControllerWriteAccess
	@PostMapping("/ui/affiliation/edit")
	public String editPost(Model model, @Valid @ModelAttribute("affiliationEditDTO") AffiliationEditDTO affiliationEditDTO, BindingResult bindingResult) throws Exception {
		if (bindingResult.hasErrors()) {
			model.addAttribute(bindingResult.getAllErrors());
			model.addAttribute("affiliationEditDTO", affiliationEditDTO);

			return "affiliation/edit";
		}

		Person person = personService.getByUuid(affiliationEditDTO.getPersonUuid());
		if (person == null) {
			log.warn("No Person associated with affiliation with uuid " + affiliationEditDTO.getUuid());
			return "redirect:/";
		}
		
		Set<String> constraintOUs = SecurityUtil.getOrgUnitUuidsFromConstraint();
		
		for (Affiliation affiliation : person.getAffiliations()) {
			if (affiliation.getUuid().equals(affiliationEditDTO.getUuid())) {
				if (constraintOUs.size() > 0 && (!constraintOUs.contains(affiliation.getOrgUnit().getUuid()) || (affiliation.getAlternativeOrgUnit() != null && !constraintOUs.contains(affiliation.getAlternativeOrgUnit().getUuid())))) {
					log.warn("Affiliation with uuid " + affiliation.getUuid() + " can not be edited by this user");
					return "redirect:/";
				}

				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
				boolean changes = false, updateStopDate = false;
				
				if (affiliation.getMaster().equals("SOFD")) {
					if (affiliation.getStopDate() != null) {
						String existingStopDate = formatter.format(affiliation.getStopDate());
						
						if (!existingStopDate.equals(affiliationEditDTO.getStopDate())) {
							updateStopDate = true;
						}
					}
					else if (StringUtils.hasLength(affiliationEditDTO.getStopDate())) {
						// existing stopDate is null, and we have a non-null value to replace it
						updateStopDate = true;
					}
					
					if (updateStopDate) {
						changes = true;
	
						if (StringUtils.hasLength(affiliationEditDTO.getStopDate())) {
							affiliation.setStopDate(formatter.parse(affiliationEditDTO.getStopDate()));
						}
						else {
							affiliation.setStopDate(null);
						}
					}
					
					if (!Objects.equals(affiliationEditDTO.getPositionName(), affiliation.getPositionName())) {
						changes = true;
						affiliation.setPositionName(affiliationEditDTO.getPositionName());
					}
					
					if (!Objects.equals(affiliationEditDTO.getVendor(), affiliation.getVendor())) {
						changes = true;
						affiliation.setVendor(affiliationEditDTO.getVendor());
					}
					
					if (!Objects.equals(affiliationEditDTO.getInternalReference(), affiliation.getInternalReference())) {
						changes = true;
						affiliation.setInternalReference(affiliationEditDTO.getInternalReference());
					}
	
					if (sofdConfiguration.getModules().getPositionDisplayName().isEnabled() && !Objects.equals(affiliationEditDTO.getPositionDisplayName(), affiliation.getPositionDisplayName())) {
						changes = true;
						affiliation.setPositionDisplayName(affiliationEditDTO.getPositionDisplayName());
					}
				}
				
				if (!Objects.equals(affiliationEditDTO.isDoNotTransferToFKOrg(), affiliation.isDoNotTransferToFkOrg())) {
					changes = true;
					affiliation.setDoNotTransferToFkOrg(affiliationEditDTO.isDoNotTransferToFKOrg());
				}

				if (changes) {
					personService.save(person);
				}
				
				break;
			}
		}

		return "redirect:/ui/person/view/" + affiliationEditDTO.getPersonUuid();
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
}
