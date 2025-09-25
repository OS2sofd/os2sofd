package dk.digitalidentity.sofd.controller.mvc;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import dk.digitalidentity.sofd.config.SofdConfiguration;
import dk.digitalidentity.sofd.controller.mvc.dto.ContactPlaceDTO;
import dk.digitalidentity.sofd.controller.mvc.dto.KleDTO;
import dk.digitalidentity.sofd.dao.model.ContactPlace;
import dk.digitalidentity.sofd.dao.model.Kle;
import dk.digitalidentity.sofd.dao.model.OrgUnit;
import dk.digitalidentity.sofd.dao.model.mapping.ContactPlaceKleMapping;
import dk.digitalidentity.sofd.dao.model.mapping.ContactPlaceOrgUnitMapping;
import dk.digitalidentity.sofd.security.RequireControllerWriteAccess;
import dk.digitalidentity.sofd.service.ContactPlaceService;
import dk.digitalidentity.sofd.service.KleService;
import dk.digitalidentity.sofd.service.OrgUnitService;
import lombok.extern.slf4j.Slf4j;

@RequireControllerWriteAccess
@Slf4j
@Controller
public class ContactPlacesController {

	@Autowired
	private SofdConfiguration config;

	@Autowired
	private ContactPlaceService contactPlacesService;

	@Autowired
	private OrgUnitService orgUnitService;

	@Autowired
	private KleService kleService;

	@GetMapping("/ui/contactplaces")
	public String list(Model model) {
		if (!config.getModules().getContactPlaces().isEnabled()) {
			return "redirect:/";
		}

		List<ContactPlaceDTO> contactPlaces = contactPlacesService.findAll().stream()
				.filter(c -> !c.isDeleted())
				.map(c -> new ContactPlaceDTO(c))
				.collect(Collectors.toList());

		model.addAttribute("contactPlaces", contactPlaces);

		return "contactplaces/list";
	}

	@GetMapping("/ui/contactplaces/create/{uuid}")
	public String create(Model model, @PathVariable(name = "uuid") String uuid, RedirectAttributes redirectAttributes) {
		if (!config.getModules().getContactPlaces().isEnabled()) {
			return "redirect:/";
		}

		OrgUnit ou = orgUnitService.getByUuid(uuid);
		if (ou == null) {
			log.warn("OrgUnit not found for UUID: " + uuid);
			return "redirect:/ui/contactplaces";
		}

		ContactPlace contactPlace = contactPlacesService.findByContactPlace(ou);
		if (contactPlace != null) {
			log.warn("ContactPlace for OrgUnit: " + uuid + " already exists.");
			redirectAttributes.addFlashAttribute("error", "Der findes allerede et henvendelssted for " + ou.getName());
			return "redirect:/ui/contactplaces";
		}

		contactPlace = new ContactPlace();
		contactPlace.setContactPlace(ou);
		contactPlace = contactPlacesService.save(contactPlace);

		return "redirect:/ui/contactplaces/edit/" + contactPlace.getId();
	}

	@GetMapping("/ui/contactplaces/edit/{id}")
	public String edit(Model model, @PathVariable(name = "id") long id) {
		if (!config.getModules().getContactPlaces().isEnabled()) {
			return "redirect:/";
		}

		ContactPlace contactPlace = contactPlacesService.findById(id);
		if (contactPlace == null) {
			log.warn("ContactPlace for id: " + id + " not found.");
			return "redirect:/ui/contactplaces";
		}
		
		List<KleDTO> kleDTOs = kleService.findAll().stream().map(k -> new KleDTO(k)).sorted(Comparator.comparing(KleDTO::getId)).collect(Collectors.toList());
		
		model.addAttribute("allKles", kleDTOs);
		model.addAttribute("contactPlace", contactPlace);

		return "contactplaces/edit";
	}

	@GetMapping("/ui/contactplaces/usersFragment/{id}")
	public String getUsersFragment(Model model, @PathVariable("id") long id) {
		if (!config.getModules().getContactPlaces().isEnabled()) {
			return "redirect:/";
		}

		ContactPlace contactPlace = contactPlacesService.findById(id);
		if (contactPlace == null) {
			log.warn("Requested ContactPlace with ID:" + id + " not found.");
			return "contactplaces/fragments/users :: content";
		}

		model.addAttribute("users", contactPlace.getUsers().stream()
				.filter(m -> !m.isDeleted())
				.map(ContactPlaceOrgUnitMapping::getOrgUnit)
				.collect(Collectors.toList()));

		return "contactplaces/fragments/users :: content";
	}

	@GetMapping("/ui/contactplaces/viewKLE/{id}")
	public String getViewKLEFragment(Model model, @PathVariable("id") long id) {
		if (!config.getModules().getContactPlaces().isEnabled()) {
			return "redirect:/";
		}

		ContactPlace contactPlace = contactPlacesService.findById(id);
		if (contactPlace == null) {
			log.warn("Requested ContactPlace with ID:" + id + " not found.");
			return "contactplaces/fragments/users :: content";
		}

		model.addAttribute("kles", contactPlace.getTasks().stream().map(ContactPlaceKleMapping::getKle).collect(Collectors.toList()));

		return "contactplaces/fragments/tasks :: contentView";
	}

	@GetMapping("/ui/contactplaces/editKLE/{id}")
	public String getEditKLEFragment(Model model, @PathVariable("id") long id) {
		if (!config.getModules().getContactPlaces().isEnabled()) {
			return "redirect:/";
		}

		ContactPlace contactPlace = contactPlacesService.findById(id);
		if (contactPlace == null) {
			log.warn("Requested ContactPlace with ID:" + id + " not found.");
			return "contactplaces/fragments/users :: content";
		}

		List<String> kleSelected = contactPlace.getTasks().stream().map(ContactPlaceKleMapping::getKle).map(Kle::getCode).collect(Collectors.toList());
		model.addAttribute("kleSelected", kleSelected);

		return "contactplaces/fragments/tasks :: contentEdit";
	}

	@DeleteMapping("/ui/contactplaces/delete/{id}")
	@ResponseBody
	public ResponseEntity<String> deleteContactPlace(@PathVariable(name = "id") long id) {
		if (!config.getModules().getContactPlaces().isEnabled()) {
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}

		ContactPlace contactPlace = contactPlacesService.findById(id);
		if (contactPlace == null) {
			log.warn("Requested ContactPlace with ID:" + id + " not found.");
			return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
		}

		contactPlace.setDeleted(true);
		contactPlace.setSynchronizedToOrganisation(false);
		contactPlacesService.save(contactPlace);

		return new ResponseEntity<>(HttpStatus.OK);
	}
}
